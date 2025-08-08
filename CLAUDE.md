# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test
- `./gradlew build` - Build the plugin
- `./gradlew check` - Run tests and code analysis
- `./gradlew test` - Run unit tests only
- `./gradlew test --tests "*parsing*"` - Run OGNL parsing tests (40 tests, ✅ passing)
- `./gradlew test -x rat` - Run tests excluding Apache RAT license checks
- `./gradlew runIdeForUiTests` - Launch IDE for UI testing with robot server on port 8082
- `./gradlew koverHtmlReport` - Generate code coverage reports

### Post-Migration Issues (Gradle Plugin 2.x)
- Most OGNL parsing tests fixed (40/40 ✅)
- Some integration and DOM tests may still fail due to test framework changes
- Property-based tests (OgnlCodeInsightSanityTest) may need different approach

### Development and Debugging
- `./gradlew runIde` - Run IntelliJ IDEA with the plugin for development/debugging
- `./gradlew buildPlugin` - Build plugin distribution
- `./gradlew runPluginVerifier` - Verify plugin compatibility against specified IntelliJ IDEs

### Code Quality
- `./gradlew runInspections` - Run Qodana code quality inspections (requires Docker)
- `./gradlew rat` - Run Apache RAT license check

## Project Architecture

### Core Structure
This is an IntelliJ IDEA plugin for Apache Struts 2 framework development support, written in Java and Kotlin.

**Main Package Structure:**
- `com.intellij.struts2` - Core plugin functionality
- `com.intellij.lang.ognl` - OGNL language support (Object-Graph Navigation Language)

### Key Components

**DOM Model (`com.intellij.struts2.dom`)**
- `struts/` - Struts XML configuration DOM models
- `validator/` - Validation XML DOM models  
- `params/` - Parameter handling and conversion
- `inspection/` - Model validation and inspections

**Framework Integration (`com.intellij.struts2.facet`)**
- `StrutsFacet` - Framework detection and configuration
- `StrutsFrameworkDetector` - Auto-detection of Struts projects
- UI components for facet configuration

**Language Features**
- **OGNL Support** - Complete language implementation with lexer, parser, highlighting
- **JSP Integration** - Tag library support, OGNL injection in JSP
- **FreeMarker/Velocity** - Template engine integrations
- **Groovy Support** - Action annotation processing

**IDE Features**
- **Navigation** - Go to symbols, related actions
- **Code Completion** - Action names, results, interceptors
- **Inspections** - Configuration validation, hardcoded URL detection
- **Structure View** - Struts configuration and validation file structure
- **Graph View** - Visual representation of action flows

### Template Engine Support
The plugin supports multiple view technologies:
- JSP with Struts tag libraries
- FreeMarker templates with Struts integration
- Velocity templates
- JavaScript and CSS injection in templates

### Testing Framework
- Uses IntelliJ Platform test framework
- Functional tests in `src/test/java`
- Test data in `src/test/testData`
- UI tests supported via IntelliJ UI Test Robot

### Build Configuration
- Gradle-based build with Kotlin DSL
- IntelliJ Platform Gradle Plugin 2.7.0
- Supports IntelliJ IDEA Ultimate 2024.2+
- Java 21 toolchain requirement
- Code coverage via Kover plugin

## IntelliJ Platform Upgrade Guide

This section documents the process for upgrading the plugin to support newer versions of IntelliJ Platform.

### Upgrading to IntelliJ Platform 2024.2

**Prerequisites:**
- IntelliJ Platform Gradle Plugin 2.0+ (migration already completed)
- Java 21 required (2024.2+ requirement)
- Gradle 8.5+ running on Java 17+

**Files to Update:**

Version Format: {BRANCH}.{BUILD}.{FIX}

- 241 = IntelliJ Platform branch (2024.1)
- 18968 = Build number within that branch
- 1 = Fix/patch version

Meaning:
- This plugin version targets IntelliJ IDEA 2024.1 platform
- Build 18968 corresponds to a specific IntelliJ build
- Version 1 indicates first release for this build

Context:
The "untagged" prefix suggests this was an automated release draft created by your build workflow, but the tag URL appears to be truncated or no longer accessible (404 error).

IntelliJ Version Mapping:
- 241.x = IntelliJ IDEA 2024.1
- 242.x = IntelliJ IDEA 2024.2
- 243.x = IntelliJ IDEA 2024.3
- 251.x = IntelliJ IDEA 2025.1
- 252.x = IntelliJ IDEA 2025.2

This versioning ensures plugin compatibility with specific IntelliJ Platform versions and helps users identify which IDE version the plugin supports.

#### 1. `gradle.properties`
```properties
# Platform version
platformVersion = 2024.2

# Build number ranges (2024.2 = 242)
pluginSinceBuild = 242
pluginUntilBuild = 242.*

# Plugin version should match platform
pluginVersion = 242.18968.1  # Use 242.x.y format

# where x represents the previous build number plus 1, y supposed be set to 1
```

#### 2. `build.gradle.kts`
```kotlin
// Update Java toolchain
kotlin {
    jvmToolchain(21)  # Java 21 required for 2024.2+
}

// Update Qodana plugin version to match platform
id("org.jetbrains.qodana") version "2024.2.6"
```

#### 3. `.github/workflows/build.yml`
```yaml
# Update Java version in all jobs
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    distribution: zulu
    java-version: 21  # Changed from 17

# Update Qodana action version
- name: Qodana - Code Inspection
  uses: JetBrains/qodana-action@v2024.2.6

# Add disk space management for resource-intensive jobs (inspectCode, verify)
- name: Maximize Build Space
  uses: jlumbroso/free-disk-space@v1.3.1
  with:
    tool-cache: false
    large-packages: false
```

#### 4. `.github/workflows/release.yml`
```yaml
# Update Java version
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    distribution: zulu
    java-version: 21  # Changed from 17
```

#### 5. `qodana.yml` (update existing file)
```yaml
version: "1.0"
linter: jetbrains/qodana-jvm-community:2024.2  # Match platform version
projectJDK: 21  # Match Java requirement
exclude:
  - name: All
    paths:
      - .qodana
      - build
      - gradle
      - gradlew
      - gradlew.bat
      - src/test/testData
include:
  - name: Root
    paths:
      - src/main/java
      - src/main/resources
```

#### 6. API Compatibility Fixes
Check and fix any deprecated/removed APIs by reviewing the [API Changes List](https://plugins.jetbrains.com/docs/intellij/api-changes-list-2024.html):

**Example from 2024.2 upgrade:**
```java
// Before (removed in 2024.2)
return WebUtilImpl.isWebFacetConfigurationContainingFiles(underlying, files);

// After (compatible alternative)
return underlying instanceof WebFacetConfiguration;
```

#### 7. `CHANGELOG.md`
Document the upgrade:
```markdown
### Changed
- Update `platformVersion` to `2024.2`
- Change since/until build to `242-242.*` (2024.2)
- Upgrade Java toolchain from 17 to 21 (required by IntelliJ 2024.2)
- Update GitHub Actions workflows to use Java 21
- Fix API compatibility issues for IntelliJ 2024.2
- Dependencies - upgrade plugin versions to match 2024.2
```

### General Upgrade Process

**Step 1: Check Requirements**
1. Review [JetBrains migration documentation](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-migration.html)
2. Check Java version requirements for target platform
3. Verify IntelliJ Platform Gradle Plugin version compatibility

**Step 2: Update Configuration Files**
1. Update `gradle.properties` with new platform version and build numbers
2. Update `build.gradle.kts` with correct Java toolchain and plugin versions
3. Update GitHub Actions workflows with matching Java versions
4. Update `qodana.yml` with corresponding linter version

**Step 3: Fix API Compatibility**
1. Review [API Changes List](https://plugins.jetbrains.com/docs/intellij/api-changes-list-2024.html) for breaking changes
2. Update deprecated/removed API calls
3. Test compilation and runtime compatibility

**Step 4: Verification**
1. **Build Test**: `./gradlew build` - Ensure compilation succeeds
2. **Unit Tests**: `./gradlew test -x rat` - Verify no API compatibility issues  
3. **Plugin Verifier**: `./gradlew runPluginVerifier` - Check compatibility against target IDEs
4. **Qodana Check**: Verify code quality analysis runs without version warnings

**Step 5: Documentation**
1. **ALWAYS** update `CHANGELOG.md` immediately when upgrading dependencies - add entries under the `[Unreleased]` section in the format: `- Dependencies - upgrade <plugin-name> to <version>`
2. Update this guide with any new findings or issues
3. Document any plugin-specific compatibility fixes

### Common Issues & Solutions

**Java Version Mismatch**
- Error: `sourceCompatibility='17' but IntelliJ Platform requires sourceCompatibility='21'`
- Solution: Update `jvmToolchain()` in `build.gradle.kts` and all GitHub Actions workflows

**Qodana Version Warnings**
- Error: `You are running a Qodana linter without an exact version tag`
- Solution: Update `qodana.yml` with specific linter version matching platform

**API Compatibility Issues**
- Error: `cannot find symbol` for removed methods
- Solution: Check API changes documentation and replace with compatible alternatives

**Kotlin K2 Mode**
- Java-based plugins automatically support K2 mode (no migration needed)
- If using Kotlin APIs, may need migration to Analysis API

### Migration Resources
- [IntelliJ Platform Migration Guide](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-migration.html)
- [API Changes List](https://plugins.jetbrains.com/docs/intellij/api-changes-list-2024.html)
- [Platform Gradle Plugin 2.0](https://blog.jetbrains.com/platform/2024/07/intellij-platform-gradle-plugin-2-0/)
- [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
```

## Claude Guidance

### Development Workflow
- Always propose updating @CLAUDE.md