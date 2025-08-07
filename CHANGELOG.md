<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Apache Struts IntelliJ Plugin Changelog

## [Unreleased]

### Changed

- Update `platformVersion` to `2024.2`
- Change since/until build to `242-242.*` (2024.2)
- Upgrade Java toolchain from 17 to 21 (required by IntelliJ 2024.2)
- Update GitHub Actions workflows to use Java 21
- Fix `WebUtilImpl.isWebFacetConfigurationContainingFiles` API compatibility issue for IntelliJ 2024.2
- Dependencies - upgrade `org.jetbrains.intellij.platform` to `2.7.0`

### Added

- [WW-5558](https://issues.apache.org/jira/browse/WW-5558) Support for new Struts 7 packages

## [2.0.1] - 2024-08-09

### Changed

- Update `platformVersion` to `2023.3.7`
- Change since/until build to `233-242.*` (2023.3-2024.2.*)
- Cleanup registering the `runIdeForUiTests` task
- Dependencies - upgrade `org.jetbrains.intellij.platform` to `2.0.1`
- Dependencies - upgrade `org.jetbrains.kotlin.jvm` to `1.9.25`
- Dependencies - upgrade `org.jetbrains.kotlinx.kover` to `0.8.3`
- Dependencies - upgrade `org.jetbrains.qodana` to `2024.1.9`

[Unreleased]: https://github.com/JetBrains/intellij-platform-plugin-template/compare/v2.0.1...HEAD
[2.0.1]: https://github.com/JetBrains/intellij-platform-plugin-template/compare/v2.0.0...v2.0.1
