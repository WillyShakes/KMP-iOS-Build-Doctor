package org.redesnac.wilfriedmbouenda.plugin.diagnostics

import org.redesnac.wilfriedmbouenda.plugin.engine.BuildAction
import java.nio.file.Path

enum class DiagnosticSeverity {
    OK,
    INFO,
    WARNING,
    CRITICAL
}

data class DiagnosticSignal(
    val id: String,
    val title: String,
    val message: String,
    val severity: DiagnosticSeverity,
    val action: BuildAction? = null
)

data class BuildDiagnosis(
    val recommendation: BuildAction,
    val confidenceLabel: String,
    val summary: String,
    val signals: List<DiagnosticSignal>,
    val iosTaskPath: String,
)

data class ProjectDiagnosticReport(
    val projectPath: Path?,
    val signals: List<DiagnosticSignal>,
    val recommendedIosTaskPath: String = ":shared:linkDebugFrameworkIosSimulatorArm64",
)

data class KmpAnalysis(
    val sharedModuleName: String,
    val iosTaskPath: String,
    val signals: List<DiagnosticSignal>,
)

data class ProjectEnvironment(
    val projectPath: Path?,
    val homePath: Path,
) {
    companion object {
        fun from(project: com.intellij.openapi.project.Project): ProjectEnvironment {
            return ProjectEnvironment(
                projectPath = project.basePath?.let { Path.of(it) },
                homePath = Path.of(System.getProperty("user.home")),
            )
        }
    }
}
