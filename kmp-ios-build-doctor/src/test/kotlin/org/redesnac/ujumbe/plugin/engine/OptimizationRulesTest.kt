package org.redesnac.ujumbe.plugin.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import org.redesnac.ujumbe.plugin.diagnostics.DiagnosticSeverity
import org.redesnac.ujumbe.plugin.diagnostics.DiagnosticSignal
import org.redesnac.ujumbe.plugin.diagnostics.ProjectDiagnosticReport

class OptimizationRulesTest {
    @Test
    fun `recommends iOS build when no warning action is present`() {
        val diagnosis = OptimizationRules.recommend(
            ProjectDiagnosticReport(
                projectPath = null,
                signals = listOf(
                    DiagnosticSignal(
                        id = "kmp.ready",
                        title = "KMP ready",
                        message = "Ready",
                        severity = DiagnosticSeverity.OK,
                        action = BuildAction.BUILD_IOS,
                    )
                ),
                recommendedIosTaskPath = ":shared:linkDebugFrameworkIosSimulatorArm64",
            )
        )

        assertEquals(BuildAction.BUILD_IOS, diagnosis.recommendation)
        assertEquals("FAST BUILD", diagnosis.confidenceLabel)
        assertEquals(":shared:linkDebugFrameworkIosSimulatorArm64", diagnosis.iosTaskPath)
    }

    @Test
    fun `prioritizes Kotlin Native cache repair before iOS build`() {
        val diagnosis = OptimizationRules.recommend(
            ProjectDiagnosticReport(
                projectPath = null,
                signals = listOf(
                    DiagnosticSignal(
                        id = "konan.cache.corrupted",
                        title = "K/N cache corrupt",
                        message = "Clear K/N cache",
                        severity = DiagnosticSeverity.WARNING,
                        action = BuildAction.CLEAR_KONAN_CACHE,
                    )
                ),
                recommendedIosTaskPath = ":shared:linkDebugFrameworkIosSimulatorArm64",
            )
        )

        assertEquals(BuildAction.CLEAR_KONAN_CACHE, diagnosis.recommendation)
        assertEquals("FULL REBUILD REQUIRED", diagnosis.confidenceLabel)
        assertEquals("Clear K/N cache", diagnosis.summary)
    }

    @Test
    fun `recommends Android build before iOS when artifacts are missing`() {
        val diagnosis = OptimizationRules.recommend(
            ProjectDiagnosticReport(
                projectPath = null,
                signals = listOf(
                    DiagnosticSignal(
                        id = "android-artifacts-missing",
                        title = "Android artifacts missing",
                        message = "Build Android first",
                        severity = DiagnosticSeverity.WARNING,
                        action = BuildAction.BUILD_ANDROID_FIRST,
                    )
                ),
                recommendedIosTaskPath = ":shared:linkDebugFrameworkIosSimulatorArm64",
            )
        )

        assertEquals(BuildAction.BUILD_ANDROID_FIRST, diagnosis.recommendation)
        assertEquals("MEDIUM BUILD", diagnosis.confidenceLabel)
    }
}
