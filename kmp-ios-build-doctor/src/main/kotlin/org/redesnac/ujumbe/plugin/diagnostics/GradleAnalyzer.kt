package org.redesnac.ujumbe.plugin.diagnostics

import org.redesnac.ujumbe.plugin.engine.BuildAction
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class GradleAnalyzer(
    private val nowMillis: Long = System.currentTimeMillis(),
) {
    fun analyze(root: Path?): List<DiagnosticSignal> {
        if (root == null) return emptyList()
        val signals = mutableListOf<DiagnosticSignal>()

        val gradleDir = root.resolve(".gradle")
        val buildDir = root.resolve("build")
        val gradleProperties = root.resolve("gradle.properties")

        if (gradleDir.exists()) {
            val cacheSize = FileSystemUtils.directorySizeBytes(gradleDir)
            val hasBrokenIncrementalState = FileSystemUtils.walk(gradleDir, maxDepth = 5)
                .any { it.name.endsWith(".failed") || it.name.contains("corrupt", ignoreCase = true) }
            if (hasBrokenIncrementalState) {
                signals += DiagnosticSignal(
                    id = "gradle-cache-corrupted",
                    severity = DiagnosticSeverity.WARNING,
                    title = "Gradle incremental state may be broken",
                    message = "Found failed or corruption markers under .gradle. A clean is the fastest safe reset.",
                    action = BuildAction.CLEAN_GRADLE,
                )
            }
            if (cacheSize > GRADLE_CACHE_SIZE_WARNING_BYTES) {
                signals += DiagnosticSignal(
                    id = "gradle-cache-large",
                    severity = DiagnosticSeverity.WARNING,
                    title = "Gradle project cache is large",
                    message = ".gradle is ${FileSystemUtils.formatBytes(cacheSize)}, which can slow configuration and stale-state checks.",
                    action = BuildAction.CLEAN_GRADLE,
                )
            }

            val newestDaemonState = FileSystemUtils.newestModifiedTime(gradleDir.resolve("daemon"))?.toMillis()
            if (newestDaemonState != null && nowMillis - newestDaemonState > STALE_DAEMON_MILLIS) {
                signals += DiagnosticSignal(
                    id = "gradle-daemon-stale",
                    severity = DiagnosticSeverity.WARNING,
                    title = "Gradle daemon state looks stale",
                    message = "Daemon metadata has not changed recently; restarting Gradle is a faster first fix than a full rebuild.",
                    action = BuildAction.RESTART_GRADLE,
                )
            }
        }

        if (buildDir.exists() && buildDir.isDirectory()) {
            val buildSize = FileSystemUtils.directorySizeBytes(buildDir)
            if (buildSize > BUILD_DIR_SIZE_WARNING_BYTES) {
                signals += DiagnosticSignal(
                    id = "gradle-build-large",
                    severity = DiagnosticSeverity.INFO,
                    title = "Root build output is large",
                    message = "The root build directory is ${FileSystemUtils.formatBytes(buildSize)}.",
                )
            }
        }

        if (gradleProperties.exists() && Files.isRegularFile(gradleProperties)) {
            val properties = runCatching { Files.readString(gradleProperties) }.getOrDefault("")
            if (!properties.contains("org.gradle.configuration-cache=true")) {
                signals += DiagnosticSignal(
                    id = "gradle-configuration-cache-disabled",
                    severity = DiagnosticSeverity.INFO,
                    title = "Gradle configuration cache is not explicitly enabled",
                    message = "Enabling configuration cache can reduce repeated KMP build setup cost when the project supports it.",
                )
            }
        }

        return signals
    }

    private companion object {
        const val GRADLE_CACHE_SIZE_WARNING_BYTES = 1_500L * 1024L * 1024L
        const val BUILD_DIR_SIZE_WARNING_BYTES = 1_000L * 1024L * 1024L
        const val STALE_DAEMON_MILLIS = 12L * 60L * 60L * 1000L
    }
}
