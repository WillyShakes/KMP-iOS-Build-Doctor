package org.redesnac.ujumbe.plugin.diagnostics

import org.redesnac.ujumbe.plugin.engine.BuildAction
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class KmpAnalyzer {
    fun analyze(root: Path?): KmpAnalysis {
        if (root == null) {
            return KmpAnalysis(
                sharedModuleName = "shared",
                iosTaskPath = ":shared:linkDebugFrameworkIosSimulatorArm64",
                signals = emptyList(),
            )
        }

        val gradleFiles = FileSystemUtils.walk(root, maxDepth = 5)
            .filter { Files.isRegularFile(it) }
            .filter { it.name.endsWith(".gradle") || it.name.endsWith(".gradle.kts") }
            .take(120)
            .toList()

        val kmpFiles = gradleFiles.filter { file ->
            val text = FileSystemUtils.readTextSafely(file)
            text.contains("org.jetbrains.kotlin.multiplatform") || text.contains("kotlin(\"multiplatform\")")
        }
        val sharedModule = resolveSharedModule(root, kmpFiles)
        val iosTask = resolveIosTask(root, sharedModule, kmpFiles)
        val signals = mutableListOf<DiagnosticSignal>()

        if (kmpFiles.isEmpty()) {
            signals += DiagnosticSignal(
                id = "kmp.not-detected",
                title = "KMP plugin not detected",
                message = "No Kotlin Multiplatform Gradle plugin was found in the scanned build files.",
                severity = DiagnosticSeverity.INFO,
            )
        }

        val hasIosTarget = kmpFiles.any { file ->
            val text = FileSystemUtils.readTextSafely(file)
            IOS_TARGET_PATTERNS.any { it.containsMatchIn(text) }
        }
        if (kmpFiles.isNotEmpty() && !hasIosTarget) {
            signals += DiagnosticSignal(
                id = "kmp.ios-target-missing",
                title = "iOS target not detected",
                message = "The project uses KMP, but no ios(), iosArm64(), iosX64(), or iosSimulatorArm64() target was found.",
                severity = DiagnosticSeverity.WARNING,
            )
        }

        val aarPath = root.resolve(sharedModule).resolve("build/outputs/aar")
        val moduleBuildPath = root.resolve(sharedModule).resolve("build")
        if (moduleBuildPath.exists() && moduleBuildPath.isDirectory() && !FileSystemUtils.containsAnyFile(aarPath)) {
            signals += DiagnosticSignal(
                id = "android-artifacts-missing",
                title = "Android artifacts are missing",
                message = "$sharedModule/build/outputs/aar is empty. Build Android first if iOS packaging depends on Android outputs.",
                severity = DiagnosticSeverity.WARNING,
                action = BuildAction.BUILD_ANDROID_FIRST,
            )
        }

        if (kmpFiles.isNotEmpty() && signals.none { it.id.startsWith("kmp.") || it.id == "android-artifacts-missing" }) {
            signals += DiagnosticSignal(
                id = "kmp.ready",
                title = "KMP iOS target is ready",
                message = "Detected KMP iOS target. Recommended task: ./gradlew $iosTask",
                severity = DiagnosticSeverity.OK,
                action = BuildAction.BUILD_IOS,
            )
        }

        return KmpAnalysis(
            sharedModuleName = sharedModule,
            iosTaskPath = iosTask,
            signals = signals,
        )
    }

    private fun resolveSharedModule(root: Path, kmpFiles: List<Path>): String {
        return sequenceOf("shared", "composeApp", "common")
            .firstOrNull { root.resolve(it).isDirectory() }
            ?: kmpFiles
                .mapNotNull { file ->
                    val relativePath = root.relativize(file)
                    if (relativePath.nameCount > 1) relativePath.getName(0).toString() else null
                }
                .firstOrNull()
            ?: "shared"
    }

    private fun resolveIosTask(root: Path, sharedModule: String, kmpFiles: List<Path>): String {
        val taskSuffix = kmpFiles
            .map { FileSystemUtils.readTextSafely(it) }
            .firstNotNullOfOrNull { text ->
                when {
                    Regex("""\biosSimulatorArm64\s*\(""").containsMatchIn(text) -> "linkDebugFrameworkIosSimulatorArm64"
                    Regex("""\biosX64\s*\(""").containsMatchIn(text) -> "linkDebugFrameworkIosX64"
                    Regex("""\biosArm64\s*\(""").containsMatchIn(text) -> "linkDebugFrameworkIosArm64"
                    else -> null
                }
            }
            ?: "linkDebugFrameworkIosSimulatorArm64"

        val isRootModule = kmpFiles.any { it.parent == root }
        return if (isRootModule && sharedModule == "shared" && !root.resolve(sharedModule).isDirectory()) {
            ":$taskSuffix"
        } else {
            ":$sharedModule:$taskSuffix"
        }
    }

    private companion object {
        val IOS_TARGET_PATTERNS = listOf(
            Regex("""\bios\s*\("""),
            Regex("""\biosArm64\s*\("""),
            Regex("""\biosSimulatorArm64\s*\("""),
            Regex("""\biosX64\s*\("""),
        )
    }
}
