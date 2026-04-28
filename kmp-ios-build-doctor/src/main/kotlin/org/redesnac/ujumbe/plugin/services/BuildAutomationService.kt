package org.redesnac.ujumbe.plugin.services

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.ConsoleViewImpl
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.redesnac.ujumbe.plugin.engine.BuildAction
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class BuildAutomationService(private val project: Project) {
    fun run(action: BuildAction, iosTaskPath: String, console: ConsoleViewImpl? = null) {
        val basePath = project.basePath ?: return
        val command = commandFor(action, iosTaskPath)
        console?.print("> ${command.joinToString(" ")}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        try {
            val commandLine = GeneralCommandLine(command)
                .withWorkDirectory(basePath)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            val handler = OSProcessHandler(commandLine)
            console?.attachToProcess(handler)
            ProcessTerminatedListener.attach(handler)
            handler.startNotify()
        } catch (exception: ExecutionException) {
            console?.print("Failed to run action: ${exception.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    private fun commandFor(action: BuildAction, iosTaskPath: String): List<String> {
        val home = Path.of(System.getProperty("user.home"))
        return when (action) {
            BuildAction.BUILD_IOS -> listOf("./gradlew", iosTaskPath)
            BuildAction.BUILD_ANDROID_FIRST -> listOf("./gradlew", "assembleDebug")
            BuildAction.CLEAN_GRADLE -> listOf("./gradlew", "clean")
            BuildAction.RESTART_GRADLE -> listOf("./gradlew", "--stop")
            BuildAction.CLEAR_KONAN_CACHE -> listOf("rm", "-rf", home.resolve(".konan/cache").toString())
            BuildAction.CLEAR_DERIVED_DATA -> listOf("sh", "-c", "rm -rf \"$HOME/Library/Developer/Xcode/DerivedData\"/*")
            BuildAction.FULL_REBUILD -> listOf(
                "sh",
                "-c",
                "./gradlew --stop && ./gradlew clean && rm -rf \"$HOME/.konan/cache\" \"$HOME/Library/Developer/Xcode/DerivedData\"/* && ./gradlew assembleDebug && ./gradlew $iosTaskPath"
            )
        }
    }

    companion object {
        fun getInstance(project: Project): BuildAutomationService = project.service()
    }
}
