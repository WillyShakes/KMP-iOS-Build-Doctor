package org.redesnac.ujumbe.plugin.services

import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import org.redesnac.ujumbe.plugin.engine.BuildAction

class BuildCommandPlannerTest {
    private val planner = BuildCommandPlanner()

    @Test
    fun `uses project Gradle wrapper when available`() {
        val root = createTempDirectory("doctor-command")
        val wrapper = root.resolve("gradlew").createFile()

        val command = planner.commandFor(BuildAction.BUILD_IOS, ":shared:linkDebugFrameworkIosSimulatorArm64", root)

        assertEquals(listOf(wrapper.toAbsolutePath().toString(), ":shared:linkDebugFrameworkIosSimulatorArm64"), command)
    }

    @Test
    fun `falls back to gradle executable when wrapper is missing`() {
        val root = createTempDirectory("doctor-command")

        val command = planner.commandFor(BuildAction.CLEAN_GRADLE, ":shared:linkDebugFrameworkIosSimulatorArm64", root)

        assertEquals(listOf("gradle", "clean"), command)
    }
}
