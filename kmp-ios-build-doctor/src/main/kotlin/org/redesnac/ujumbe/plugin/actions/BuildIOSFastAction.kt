package org.redesnac.ujumbe.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import org.redesnac.ujumbe.plugin.engine.DiagnosticEngine
import org.redesnac.ujumbe.plugin.services.BuildAutomationService

class BuildIOSFastAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            val diagnosis = DiagnosticEngine.getInstance(project).analyze()
            ApplicationManager.getApplication().invokeLater {
                BuildAutomationService.getInstance(project).run(diagnosis.recommendation, diagnosis.iosTaskPath)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }
}
