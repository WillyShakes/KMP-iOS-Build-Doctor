package org.redesnac.wilfriedmbouenda.plugin.services

import org.redesnac.wilfriedmbouenda.plugin.engine.BuildAction
import java.nio.file.Files
import java.nio.file.Path

object BuildCommandPlanner {
    fun commandFor(action: BuildAction, iosTaskPath: String, projectPath: Path): List<String> {
        val home = Path.of(System.getProperty("user.home"))
        val gradleCommand = gradleCommand(projectPath)
        return when (action) {
            BuildAction.BUILD_IOS -> gradleCommand + iosTaskPath
            BuildAction.BUILD_ANDROID_FIRST -> gradleCommand + "assembleDebug"
            BuildAction.CLEAN_GRADLE -> gradleCommand + "clean"
            BuildAction.RESTART_GRADLE -> gradleCommand + "--stop"
            BuildAction.CLEAR_KONAN_CACHE -> listOf("rm", "-rf", home.resolve(".konan/cache").toString())
            BuildAction.CLEAR_DERIVED_DATA -> listOf("sh", "-c", "rm -rf \"\$HOME/Library/Developer/Xcode/DerivedData\"/*")
            BuildAction.FULL_REBUILD -> listOf(
                "sh",
                "-c",
                "${gradleCommand.joinToString(" ")} --stop && " +
                    "${gradleCommand.joinToString(" ")} clean && " +
                    "rm -rf \"\$HOME/.konan/cache\" \"\$HOME/Library/Developer/Xcode/DerivedData\"/* && " +
                    "${gradleCommand.joinToString(" ")} assembleDebug && " +
                    "${gradleCommand.joinToString(" ")} $iosTaskPath"
            )
        }
    }

    private fun gradleCommand(projectPath: Path): List<String> {
        val wrapper = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            projectPath.resolve("gradlew.bat")
        } else {
            projectPath.resolve("gradlew")
        }

        return if (Files.isRegularFile(wrapper)) {
            listOf(wrapper.toAbsolutePath().toString())
        } else {
            listOf("gradle")
        }
    }
}
