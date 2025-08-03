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
- IntelliJ Platform Gradle Plugin 2.1.0
- Supports IntelliJ IDEA Ultimate 2023.3+
- Java 17 toolchain requirement
- Code coverage via Kover plugin