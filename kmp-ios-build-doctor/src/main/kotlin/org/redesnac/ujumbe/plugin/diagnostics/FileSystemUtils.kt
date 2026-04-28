package org.redesnac.ujumbe.plugin.diagnostics

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.streams.asSequence

internal object FileSystemUtils {
    fun directorySizeBytes(path: Path, maxDepth: Int = 8): Long {
        if (!path.exists()) return 0L

        return Files.walk(path, maxDepth).use { stream ->
            stream
                .asSequence()
                .filter { Files.isRegularFile(it) }
                .sumOf { runCatching { Files.size(it) }.getOrDefault(0L) }
        }
    }

    fun newestModifiedTime(path: Path, maxDepth: Int = 6): FileTime? {
        if (!path.exists()) return null

        return Files.walk(path, maxDepth).use { stream ->
            stream
                .asSequence()
                .mapNotNull { runCatching { Files.getLastModifiedTime(it) }.getOrNull() }
                .maxByOrNull { it.toMillis() }
        }
    }

    fun containsAnyFile(path: Path, maxDepth: Int = 4): Boolean {
        if (!path.exists()) return false

        return Files.walk(path, maxDepth).use { stream ->
            stream.asSequence().any { Files.isRegularFile(it) }
        }
    }

    fun walk(path: Path, maxDepth: Int): Sequence<Path> {
        if (!path.exists()) return emptySequence()

        return Files.walk(path, maxDepth).use { stream ->
            stream.asSequence().toList().asSequence()
        }
    }

    fun formatBytes(bytes: Long): String {
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) {
            "$bytes ${units[unitIndex]}"
        } else {
            "%.1f %s".format(value, units[unitIndex])
        }
    }

    fun readTextSafely(path: Path): String {
        return runCatching { Files.readString(path) }.getOrDefault("")
    }
}
