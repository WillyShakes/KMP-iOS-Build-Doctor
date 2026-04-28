package org.redesnac.ujumbe.plugin.diagnostics

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import org.redesnac.ujumbe.plugin.engine.BuildAction

class KonanAnalyzer {
    fun analyze(homePath: Path): List<DiagnosticSignal> {
        val konanPath = homePath.resolve(".konan")
        val cachePath = konanPath.resolve("cache")

        if (!konanPath.exists()) {
            return listOf(DiagnosticSignal(
                id = "konan.cache.missing",
                title = "Kotlin/Native cache not initialized",
                message = "~/.konan does not exist yet. The first iOS build may spend extra time downloading toolchains.",
                severity = DiagnosticSeverity.INFO
            ))
        }

        val cacheSize = FileSystemUtils.directorySizeBytes(cachePath)
        val corruptedMarkers = findCorruptionMarkers(konanPath)
        val hasPartialDownloads = FileSystemUtils.walk(konanPath, maxDepth = 3)
            .any { path ->
                path.toFile().isFile &&
                    (path.fileName.toString().endsWith(".part") || path.fileName.toString().endsWith(".tmp"))
            }

        return listOf(when {
            corruptedMarkers.isNotEmpty() -> DiagnosticSignal(
                id = "konan.cache.corrupted",
                title = "Kotlin/Native cache may be corrupted",
                message = "Found suspicious K/N cache marker: ${corruptedMarkers.first()}",
                severity = DiagnosticSeverity.WARNING,
                action = BuildAction.CLEAR_KONAN_CACHE,
            )
            hasPartialDownloads -> DiagnosticSignal(
                id = "konan.cache.partial",
                title = "Kotlin/Native cache has partial downloads",
                message = "Partial artifacts under ~/.konan can break Kotlin/Native linking.",
                severity = DiagnosticSeverity.WARNING,
                action = BuildAction.CLEAR_KONAN_CACHE,
            )
            cacheSize > KONAN_CACHE_WARNING_BYTES -> DiagnosticSignal(
                id = "konan.cache.large",
                title = "Kotlin/Native cache is large",
                message = "Cache size is ${FileSystemUtils.formatBytes(cacheSize)}. Clearing it may help if linker failures repeat.",
                severity = DiagnosticSeverity.INFO
            )
            else -> DiagnosticSignal(
                id = "konan.cache.ok",
                title = "Kotlin/Native cache looks healthy",
                message = "No corruption markers or partial downloads found.",
                severity = DiagnosticSeverity.OK
            )
        })
    }

    private fun findCorruptionMarkers(root: Path): List<Path> {
        if (!root.exists() || !root.isDirectory()) return emptyList()

        return FileSystemUtils.walk(root, maxDepth = 4)
            .filter { it.toFile().isFile }
            .filter {
                val name = it.fileName.toString().lowercase()
                name.contains("corrupt") || name.endsWith(".failed")
            }
            .take(5)
            .toList()
    }

    private companion object {
        const val KONAN_CACHE_WARNING_BYTES = 4L * 1024L * 1024L * 1024L
    }
}
