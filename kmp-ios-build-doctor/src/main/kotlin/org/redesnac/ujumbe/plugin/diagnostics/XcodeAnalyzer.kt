package org.redesnac.ujumbe.plugin.diagnostics

import org.redesnac.ujumbe.plugin.engine.BuildAction
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class XcodeAnalyzer {
    fun analyze(homePath: Path): List<DiagnosticSignal> {
        val derivedDataPath = homePath.resolve("Library/Developer/Xcode/DerivedData")
        if (!derivedDataPath.exists()) {
            return listOf(
                DiagnosticSignal(
                    id = "xcode.derived-data.missing",
                    title = "Xcode DerivedData not found",
                    message = "No DerivedData directory was detected. This is common on clean machines.",
                    severity = DiagnosticSeverity.INFO,
                )
            )
        }

        val sizeBytes = FileSystemUtils.directorySizeBytes(derivedDataPath)
        val lastFailed = hasRecentFailureMarker(derivedDataPath)
        val signals = mutableListOf<DiagnosticSignal>()

        if (sizeBytes > DERIVED_DATA_SIZE_WARNING_BYTES) {
            signals += DiagnosticSignal(
                id = "xcode.derived-data.large",
                title = "DerivedData is large",
                message = "Xcode DerivedData is ${FileSystemUtils.formatBytes(sizeBytes)}. Clearing it can fix stale Swift/Clang state.",
                severity = DiagnosticSeverity.WARNING,
                action = BuildAction.CLEAR_DERIVED_DATA,
            )
        }

        if (lastFailed) {
            signals += DiagnosticSignal(
                id = "xcode.last-build.failed",
                title = "Recent Xcode build failure detected",
                message = "DerivedData contains recent failure logs. Clearing DerivedData is the fastest recovery path.",
                severity = DiagnosticSeverity.WARNING,
                action = BuildAction.CLEAR_DERIVED_DATA,
            )
        }

        return signals.ifEmpty {
            listOf(
                DiagnosticSignal(
                    id = "xcode.derived-data.ok",
                    title = "Xcode DerivedData looks healthy",
                    message = "DerivedData size is ${FileSystemUtils.formatBytes(sizeBytes)} and no obvious failure marker was found.",
                    severity = DiagnosticSeverity.INFO,
                )
            )
        }
    }

    private fun hasRecentFailureMarker(root: Path): Boolean {
        return FileSystemUtils.walk(root, maxDepth = 4)
            .filter { it.isRegularFile() }
            .filter { it.name.endsWith(".xcactivitylog") || it.name.endsWith(".log") }
            .take(50)
            .any { path ->
                runCatching {
                    Files.readString(path).contains("failed", ignoreCase = true) ||
                        Files.readString(path).contains("error:", ignoreCase = true)
                }.getOrDefault(false)
            }
    }

    private companion object {
        private const val DERIVED_DATA_SIZE_WARNING_BYTES = 3L * 1024L * 1024L * 1024L
    }
}
