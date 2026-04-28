package org.redesnac.wilfriedmbouenda.plugin.engine

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.redesnac.wilfriedmbouenda.plugin.diagnostics.BuildDiagnosis
import org.redesnac.wilfriedmbouenda.plugin.diagnostics.ProjectAnalyzer

@Service(Service.Level.PROJECT)
class DiagnosticEngine(private val project: Project) {
    private val projectAnalyzer = ProjectAnalyzer()

    fun analyze(): BuildDiagnosis {
        val report = projectAnalyzer.analyze(project)
        return OptimizationRules.recommend(report)
    }

    companion object {
        fun getInstance(project: Project): DiagnosticEngine = project.service()
    }
}
