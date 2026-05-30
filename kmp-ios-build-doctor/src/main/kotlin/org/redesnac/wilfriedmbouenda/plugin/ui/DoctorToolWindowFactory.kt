package org.redesnac.wilfriedmbouenda.plugin.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import org.redesnac.wilfriedmbouenda.plugin.diagnostics.BuildDiagnosis
import org.redesnac.wilfriedmbouenda.plugin.diagnostics.DiagnosticSeverity
import org.redesnac.wilfriedmbouenda.plugin.engine.DiagnosticEngine
import org.redesnac.wilfriedmbouenda.plugin.services.BuildAutomationService
import java.awt.BorderLayout
import java.awt.Font
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.Timer

class DoctorToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = DoctorPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.root, "", false)
        toolWindow.contentManager.addContent(content)
        panel.refresh()
        panel.startContinuousRefresh()
    }
}

private class DoctorPanel(private val project: Project) {
    val root: JPanel = JPanel(BorderLayout(12, 12)).apply {
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
    }

    private val statusLabel = JBLabel("Analyzing KMP iOS build health...").apply {
        font = font.deriveFont(Font.BOLD, 18f)
    }
    private val summaryArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createEmptyBorder(8, 0, 8, 0)
        background = JBColor.PanelBackground
    }
    private val signalArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = JBColor.PanelBackground
    }
    private val primaryButton = JButton("Run recommendation")
    private val refreshButton = JButton("Refresh")
    private var latestDiagnosis: BuildDiagnosis? = null

    private val timer = Timer(30_000) { refresh() }

    init {
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(primaryButton)
            add(refreshButton)
        }

        val headerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(statusLabel)
            add(Box.createVerticalStrut(8))
            add(summaryArea)
            add(Box.createVerticalStrut(8))
            add(buttonPanel)
        }

        root.add(headerPanel, BorderLayout.NORTH)
        root.add(JBScrollPane(signalArea), BorderLayout.CENTER)

        primaryButton.addActionListener {
            latestDiagnosis?.let { diagnosis ->
                project.service<BuildAutomationService>().run(diagnosis.recommendation, diagnosis.iosTaskPath)
            }
        }
        refreshButton.addActionListener { refresh() }
    }

    fun startContinuousRefresh() {
        timer.start()
    }

    fun refresh() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val diagnosis = DiagnosticEngine.getInstance(project).analyze()
            ApplicationManager.getApplication().invokeLater {
                render(diagnosis)
            }
        }
    }

    private fun render(diagnosis: BuildDiagnosis) {
        latestDiagnosis = diagnosis
        statusLabel.text = "${diagnosis.confidenceLabel}: ${diagnosis.recommendation.title}"
        summaryArea.text = diagnosis.summary
        primaryButton.text = "Run: ${diagnosis.recommendation.title}"
        signalArea.text = diagnosis.signals.joinToString(separator = "\n\n") { signal ->
            "${signal.severity.icon()} ${signal.title}\n${signal.message}"
        }
    }

    private fun DiagnosticSeverity.icon(): String {
        return when (this) {
            DiagnosticSeverity.OK -> "[OK]"
            DiagnosticSeverity.INFO -> "[INFO]"
            DiagnosticSeverity.WARNING -> "[WARN]"
            DiagnosticSeverity.CRITICAL -> "[CRITICAL]"
        }
    }
}
