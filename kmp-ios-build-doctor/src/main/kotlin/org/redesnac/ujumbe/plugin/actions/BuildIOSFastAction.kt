package org.redesnac.ujumbe.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.redesnac.ujumbe.plugin.engine.DiagnosticEngine
import org.redesnac.ujumbe.plugin.services.BuildAutomationService

class BuildIOSFastAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val diagnosis = DiagnosticEngine.getInstance(project).analyze()
        BuildAutomationService.getInstance(project).run(diagnosis.recommendation, diagnosis.iosTaskPath)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }
}
