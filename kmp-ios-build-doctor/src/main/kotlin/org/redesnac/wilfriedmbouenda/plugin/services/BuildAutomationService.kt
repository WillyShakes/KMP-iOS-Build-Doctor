package org.redesnac.wilfriedmbouenda.plugin.services

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.redesnac.wilfriedmbouenda.plugin.engine.BuildAction
import java.io.File
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class BuildAutomationService(private val project: Project) {
    fun run(action: BuildAction, iosTaskPath: String, console: ConsoleView? = null) {
        val basePath = project.basePath
        if (basePath == null) {
            console?.print("Cannot run KMP iOS Doctor action without a project directory.\n", ConsoleViewContentType.ERROR_OUTPUT)
            return
        }

        val command = BuildCommandPlanner.commandFor(action, iosTaskPath, Path.of(basePath))
        if (console == null) {
            Messages.showInfoMessage(
                project,
                "KMP iOS Doctor will run:\n\n${command.joinToString(" ")}",
                "KMP iOS Build Doctor"
            )
        }

        console?.print("> ${command.joinToString(" ")}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        try {
            val commandLine = GeneralCommandLine(command)
            commandLine.workDirectory = File(basePath)
            commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            val handler = OSProcessHandler(commandLine)
            console?.attachToProcess(handler)
            ProcessTerminatedListener.attach(handler)
            handler.startNotify()
        } catch (exception: ExecutionException) {
            console?.print("Failed to run action: ${exception.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    companion object {
        fun getInstance(project: Project): BuildAutomationService = project.service()
    }
}
