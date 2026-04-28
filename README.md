# KMP iOS Build Doctor

> A JetBrains IDE plugin that helps Kotlin Multiplatform teams get from
> "iOS build failed again" to the safest next fix.

KMP iOS Build Doctor analyzes common Kotlin Multiplatform iOS build signals
inside Android Studio and IntelliJ IDEA. It inspects the project Gradle setup,
KMP iOS targets, Kotlin/Native cache state, and Xcode DerivedData, then
recommends the smallest useful action: run the iOS framework link task, restart
Gradle, clean Gradle state, clear Kotlin/Native cache, clear DerivedData, or
build Android artifacts first.

## Why this exists

Kotlin Multiplatform iOS builds often fail for reasons that are not obvious from
one Gradle error:

- stale Gradle daemon or incremental state;
- missing KMP iOS targets;
- large or corrupted Kotlin/Native cache;
- stale Xcode DerivedData;
- missing Android artifacts in projects that share generated outputs.

This plugin turns those signals into a practical recommendation directly in the
IDE.

## Features

- **KMP project detection** for Groovy and Kotlin Gradle build files.
- **iOS target detection** for `ios()`, `iosArm64()`, `iosX64()`, and
  `iosSimulatorArm64()`.
- **Recommended Gradle task** for the detected shared module.
- **Kotlin/Native cache checks** for partial downloads and corruption markers.
- **Xcode DerivedData checks** for oversized caches and recent failure logs.
- **Tool window** with a continuously refreshed diagnostic report.
- **Tools menu action** to run the recommended build recovery command.

## Requirements

### For users

- Android Studio or another JetBrains IDE based on IntelliJ Platform 2024.3+.
- A Kotlin Multiplatform project.
- macOS with Xcode installed for real iOS builds.

### For contributors

- JDK 17 or newer.
- The checked-in Gradle wrapper (`./gradlew`).
- Internet access on the first build to download Gradle and IntelliJ Platform
  artifacts.

## Install

### From a local ZIP

Build the plugin:

```bash
./gradlew :kmp-ios-build-doctor:buildPlugin
```

Install it in Android Studio:

1. Open **Settings / Preferences**.
2. Go to **Plugins**.
3. Click the gear icon.
4. Choose **Install Plugin from Disk...**.
5. Select the ZIP from:

   ```text
   kmp-ios-build-doctor/build/distributions/
   ```

6. Restart the IDE.

### From JetBrains Marketplace

After the first version is published, install it from:

1. **Settings / Preferences > Plugins > Marketplace**.
2. Search for **KMP iOS Build Doctor**.
3. Click **Install** and restart the IDE.

## Use

1. Open a Kotlin Multiplatform project in Android Studio or IntelliJ IDEA.
2. Open the **KMP iOS Doctor** tool window on the right side of the IDE.
3. Review the status, summary, and diagnostic signals.
4. Click **Run: ...** to execute the recommended command.

You can also run the action from:

```text
Tools > KMP iOS Doctor: Fast Build
```

## What the recommendation means

| Recommendation | What it does |
| --- | --- |
| Safe to build iOS | Runs the detected Kotlin/Native framework link task. |
| Build Android first | Runs `assembleDebug` before retrying iOS work. |
| Clean Gradle | Runs `clean` to reset project build outputs. |
| Restart Gradle | Runs `--stop` to terminate stale Gradle daemons. |
| Clear Kotlin/Native cache | Removes `~/.konan/cache`. |
| Clear Xcode DerivedData | Removes Xcode DerivedData contents. |
| Full rebuild required | Stops Gradle, cleans, clears native/Xcode caches, then rebuilds. |

The plugin uses the project Gradle wrapper when available and falls back to the
`gradle` executable.

## Develop

Clone the repository and run:

```bash
./gradlew :kmp-ios-build-doctor:test
./gradlew :kmp-ios-build-doctor:buildPlugin
```

Run the plugin in a sandbox IDE:

```bash
./gradlew :kmp-ios-build-doctor:runIde
```

Useful Gradle tasks:

```bash
./gradlew :kmp-ios-build-doctor:test
./gradlew :kmp-ios-build-doctor:buildPlugin
./gradlew :kmp-ios-build-doctor:runIde
./gradlew :kmp-ios-build-doctor:verifyPlugin
```

## Test the plugin manually

1. Build the plugin ZIP:

   ```bash
   ./gradlew :kmp-ios-build-doctor:buildPlugin
   ```

2. Install the ZIP from `kmp-ios-build-doctor/build/distributions/`.
3. Open a sample KMP project with a module such as `shared` or `composeApp`.
4. Confirm the **KMP iOS Doctor** tool window appears.
5. Check these scenarios:
   - project has an iOS target and receives a safe iOS build recommendation;
   - project has no iOS target and receives a warning;
   - `~/.konan` is missing and receives an informational first-build note;
   - a fake `.failed` marker under `.gradle` produces a clean recommendation;
   - the **Tools > KMP iOS Doctor: Fast Build** action is enabled only when a
     project is open.

## Publish the first version

Before publishing, update these values in `gradle.properties`:

```properties
pluginVersion=0.1.0
pluginChangeNotes=Initial preview release with Kotlin Multiplatform iOS project diagnostics.
pluginVendor=Ujumbe Contributors
pluginVendorUrl=https://github.com/WillyShakes/KMP-iOS-Build-Doctor
```

Then verify and build:

```bash
./gradlew :kmp-ios-build-doctor:test
./gradlew :kmp-ios-build-doctor:buildPlugin
./gradlew :kmp-ios-build-doctor:verifyPlugin
```

### Publish with JetBrains Marketplace token

Create a JetBrains Marketplace permanent token, then run:

```bash
export PUBLISH_TOKEN="<your-marketplace-token>"
./gradlew :kmp-ios-build-doctor:publishPlugin
```

### Publish a signed plugin

For public Marketplace releases, configure signing secrets:

```bash
export CERTIFICATE_CHAIN="<certificate-chain>"
export PRIVATE_KEY="<private-key>"
export PRIVATE_KEY_PASSWORD="<private-key-password>"
export PUBLISH_TOKEN="<your-marketplace-token>"
./gradlew :kmp-ios-build-doctor:signPlugin
./gradlew :kmp-ios-build-doctor:publishPlugin
```

Use JetBrains Marketplace's **manual review** flow for the first public release.

## Project structure

```text
kmp-ios-build-doctor/
  src/main/kotlin/org/redesnac/ujumbe/plugin/
    actions/       IDE menu actions
    diagnostics/   Gradle, KMP, Konan, and Xcode analyzers
    engine/        recommendation rules
    services/      command planning and execution
    ui/            tool window UI
  src/main/resources/META-INF/plugin.xml
  src/test/kotlin/ focused unit tests
```

## Contribute

Contributions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Add or update tests for behavior changes.
4. Run:

   ```bash
   ./gradlew :kmp-ios-build-doctor:test
   ./gradlew :kmp-ios-build-doctor:buildPlugin
   ```

5. Open a pull request with:
   - what changed;
   - why it changed;
   - screenshots or logs for UI/build behavior when relevant.

Please keep changes focused. For diagnostics, prefer small analyzers with
deterministic, testable rules over broad shell parsing.

## License

Apache License 2.0. See [LICENSE](LICENSE).