package org.redesnac.ujumbe.plugin.diagnostics

import com.intellij.openapi.project.Project
import java.nio.file.Path

class ProjectAnalyzer(
    private val gradleAnalyzer: GradleAnalyzer = GradleAnalyzer(),
    private val kmpAnalyzer: KmpAnalyzer = KmpAnalyzer(),
    private val konanAnalyzer: KonanAnalyzer = KonanAnalyzer(),
    private val xcodeAnalyzer: XcodeAnalyzer = XcodeAnalyzer(),
) {
    fun analyze(project: Project): ProjectDiagnosticReport {
        val root = project.basePath?.let { Path.of(it) }
        return analyze(root, Path.of(System.getProperty("user.home")))
    }

    fun analyze(root: Path?, homePath: Path): ProjectDiagnosticReport {
        val homePath = Path.of(System.getProperty("user.home"))
        val kmpAnalysis = kmpAnalyzer.analyze(root)
        val signals = buildList {
            addAll(gradleAnalyzer.analyze(root))
            addAll(kmpAnalysis.signals)
            addAll(konanAnalyzer.analyze(homePath))
            addAll(xcodeAnalyzer.analyze(homePath))
        }
        return ProjectDiagnosticReport(
            projectPath = root,
            signals = signals,
            recommendedIosTaskPath = kmpAnalysis.iosTaskPath,
        )
    }
}
