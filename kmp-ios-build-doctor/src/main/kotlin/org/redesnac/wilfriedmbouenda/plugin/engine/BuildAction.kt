package org.redesnac.wilfriedmbouenda.plugin.engine

enum class BuildAction(
    val title: String,
    val description: String,
) {
    BUILD_IOS(
        title = "Safe to build iOS",
        description = "Run the Kotlin/Native framework link task for the simulator."
    ),
    BUILD_ANDROID_FIRST(
        title = "Build Android first",
        description = "Generate Android artifacts that the KMP iOS build may depend on."
    ),
    CLEAN_GRADLE(
        title = "Clean Gradle",
        description = "Reset broken Gradle incremental state with a project clean."
    ),
    RESTART_GRADLE(
        title = "Restart Gradle",
        description = "Stop stale Gradle daemons before retrying the build."
    ),
    CLEAR_KONAN_CACHE(
        title = "Clear Kotlin/Native cache",
        description = "Remove the Kotlin/Native cache when it appears corrupt or stale."
    ),
    CLEAR_DERIVED_DATA(
        title = "Clear Xcode DerivedData",
        description = "Remove Xcode DerivedData when it is very large or contains failed build residue."
    ),
    FULL_REBUILD(
        title = "Full rebuild required",
        description = "Clean Gradle state, Kotlin/Native cache, Xcode DerivedData, then rebuild."
    )
}
