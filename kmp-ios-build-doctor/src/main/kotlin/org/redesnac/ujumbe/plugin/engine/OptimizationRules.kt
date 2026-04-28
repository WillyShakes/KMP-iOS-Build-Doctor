package org.redesnac.ujumbe.plugin.engine

import org.redesnac.ujumbe.plugin.diagnostics.BuildDiagnosis
import org.redesnac.ujumbe.plugin.diagnostics.DiagnosticSeverity
import org.redesnac.ujumbe.plugin.diagnostics.ProjectDiagnosticReport

object OptimizationRules {
    fun recommend(report: ProjectDiagnosticReport): BuildDiagnosis {
        val signals = report.signals
        val warningSignals = signals.filter { it.severity == DiagnosticSeverity.WARNING || it.severity == DiagnosticSeverity.CRITICAL }
        val primaryAction = when {
            signals.any { it.action == BuildAction.CLEAN_GRADLE } -> BuildAction.CLEAN_GRADLE
            signals.any { it.action == BuildAction.CLEAR_KONAN_CACHE } -> BuildAction.CLEAR_KONAN_CACHE
            signals.any { it.action == BuildAction.CLEAR_DERIVED_DATA } -> BuildAction.CLEAR_DERIVED_DATA
            signals.any { it.action == BuildAction.BUILD_ANDROID_FIRST } -> BuildAction.BUILD_ANDROID_FIRST
            signals.any { it.action == BuildAction.RESTART_GRADLE } -> BuildAction.RESTART_GRADLE
            else -> BuildAction.BUILD_IOS
        }

        val confidenceLabel = when (primaryAction) {
            BuildAction.BUILD_IOS -> "FAST BUILD"
            BuildAction.RESTART_GRADLE,
            BuildAction.BUILD_ANDROID_FIRST,
            BuildAction.CLEAR_DERIVED_DATA -> "MEDIUM BUILD"
            BuildAction.CLEAN_GRADLE,
            BuildAction.CLEAR_KONAN_CACHE,
            BuildAction.FULL_REBUILD -> "FULL REBUILD REQUIRED"
        }

        return BuildDiagnosis(
            recommendation = primaryAction,
            confidenceLabel = confidenceLabel,
            summary = warningSignals.firstOrNull()?.message ?: "Safe to build iOS now.",
            signals = signals,
            iosTaskPath = report.recommendedIosTaskPath,
        )
    }
}
