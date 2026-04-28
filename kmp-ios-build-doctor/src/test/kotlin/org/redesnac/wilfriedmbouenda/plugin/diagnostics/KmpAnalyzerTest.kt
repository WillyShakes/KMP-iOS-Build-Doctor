package org.redesnac.wilfriedmbouenda.plugin.diagnostics

import org.redesnac.wilfriedmbouenda.plugin.engine.BuildAction
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KmpAnalyzerTest {
    @Test
    fun `detects shared module and simulator task`() {
        val root = Files.createTempDirectory("kmp-doctor-test")
        root.resolve("shared").createDirectories()
        root.resolve("shared/build.gradle.kts").writeText(
            """
            plugins {
                kotlin("multiplatform")
            }

            kotlin {
                iosSimulatorArm64()
            }
            """.trimIndent()
        )

        val analysis = KmpAnalyzer().analyze(root)

        assertEquals("shared", analysis.sharedModuleName)
        assertEquals(":shared:linkDebugFrameworkIosSimulatorArm64", analysis.iosTaskPath)
        assertTrue(analysis.signals.any { it.action == BuildAction.BUILD_IOS })
    }

    @Test
    fun `detects root KMP project without forcing shared module path`() {
        val root = Files.createTempDirectory("kmp-doctor-root-test")
        root.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
            }

            kotlin {
                iosArm64()
            }
            """.trimIndent()
        )

        val analysis = KmpAnalyzer().analyze(root)

        assertEquals("shared", analysis.sharedModuleName)
        assertEquals(":linkDebugFrameworkIosArm64", analysis.iosTaskPath)
    }
}
