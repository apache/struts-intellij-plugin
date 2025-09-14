<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Apache Struts IntelliJ Plugin Changelog

## [Unreleased]

### Changed

- Update `platformVersion` to `2025.2`
- Change since/until build to `242-252.*` (2024.2-2025.2)
- Maintain backward compatibility with IntelliJ Platform 2024.x while adding support for 2025.2
- Dependencies - upgrade `org.jetbrains.qodana` to `2025.2.1`
- Update Qodana linter to `jetbrains/qodana-jvm-community:2025.2`
- Update GitHub Actions Qodana action to `v2025.2`

### Fixed

- Fix multiple internal API compatibility issues for IntelliJ Platform 2025.2:
  - Replace `PlatformIcons` internal API with public `AllIcons.Nodes.Parameter` in `OgnlReferenceExpressionBase`
  - Replace `CharsetToolkit.getAvailableCharsets()` with standard Java `Charset.availableCharsets()` in `StrutsCoreConstantContributor`
  - Replace deprecated `InjectedLanguageUtil.findElementAtNoCommit()` with `InjectedLanguageManager.findInjectedElementAt()` in `OgnlTypedHandler`
  - Replace internal `StartupManager.runAfterOpened()` API with `StartupActivity` pattern in `StrutsFrameworkSupportProvider`
  - Add `StrutsFrameworkInitializer` implementing `StartupActivity` for proper project initialization
  - Remove `DumbService.makeDumbAware` calls causing compilation errors in `FileSetConfigurationTab`
  - Reduce internal API usage violations from 5 to 3, resolving critical plugin verification failures
- Fix package naming inconsistencies - moved OGNL language support files from `com.intellij.struts2.ognl` to correct `com.intellij.lang.ognl` package structure
- Resolve compilation errors caused by mismatched package declarations and file paths
- Restructure generated OGNL parser/lexer files to match their declared packages
- Fix OGNL lexer test data path resolution issues for IntelliJ Platform 2024.2
- Update `OgnlJavaClassCompletionContributor` to use compatible APIs (`JavaLookupElementBuilder.forClass()` instead of deprecated `JavaClassNameCompletionContributor.addAllClasses()`)
- Resolve API compatibility issues for IntelliJ Platform 2024.2 migration
- Fix DOM stub test path resolution issues - `StrutsDomStubTest` now properly resolves test data paths for IntelliJ Platform 2024.2
- Fix integration test failures - all core integration tests (DOM, FreeMarker) now pass with IntelliJ Platform 2024.2

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
