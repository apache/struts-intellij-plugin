# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test

```bash
./gradlew build                              # Build the plugin
./gradlew test -x rat                        # Run unit tests (excluding Apache RAT license checks)
./gradlew test --tests "OgnlParsingTest"     # Run specific test class
./gradlew test --tests "*Resolving*"         # Run tests matching pattern
./gradlew check                              # Run tests and code analysis
./gradlew koverHtmlReport                    # Generate code coverage report (build/reports/kover/)
```

### Development and Debugging

```bash
./gradlew runIde                             # Run IntelliJ IDEA with the plugin loaded
./gradlew runIdeForUiTests                   # Launch IDE with robot server on port 8082
./gradlew buildPlugin                        # Build plugin distribution (build/distributions/)
./gradlew runPluginVerifier                  # Verify compatibility against specified IDEs
```

### Code Quality

```bash
./gradlew runInspections                     # Run Qodana inspections (requires Docker)
./gradlew rat                                # Run Apache RAT license check
```

## Project Architecture

This is an IntelliJ IDEA Ultimate plugin for Apache Struts 2 framework, providing IDE support for struts.xml
configuration, OGNL expressions, validation files, and JSP tag libraries.

### Package Structure

- `com.intellij.struts2` - Core Struts 2 plugin functionality
- `com.intellij.lang.ognl` - OGNL language support (lexer, parser, highlighting, completion)

### Key Architectural Components

**DOM Model Layer** (`com.intellij.struts2.dom`)

- `struts/` - Struts XML configuration DOM (actions, results, interceptors, packages)
- `validator/` - Validation XML DOM models
- `params/` - Parameter handling and type conversion
- DOM converters in `impl/` packages handle reference resolution

**Framework Integration** (`com.intellij.struts2.facet`)

- `StrutsFacet` / `StrutsFacetType` - Framework facet configuration
- `StrutsFrameworkDetector` - Auto-detection of Struts projects
- `StrutsFrameworkInitializer` - Modern `ProjectActivity`-based initialization (replaces deprecated `StartupManager`)
- `FileSetConfigurationTab` - UI for configuring struts.xml file sets

**Reference Resolution** (`com.intellij.struts2.reference`)

- `StrutsReferenceContributor` - XML reference providers for struts.xml
- `jsp/` - JSP tag library references and action link resolution
- Reference contributors for various Struts tag libraries (jQuery, Bootstrap, etc.)

**OGNL Language** (`com.intellij.lang.ognl`)

- `OgnlLanguage` / `OgnlFileType` - Language definition
- `lexer/` - JFlex-based lexer (`ognl.flex`)
- `parser/` - Grammar Kit parser (`OgnlParser.bnf`)
- `psi/` - PSI element types and implementations in `impl/`
- `highlight/` - Syntax and semantic highlighting
- `completion/` - Code completion providers

**Extension Points** (defined in `plugin.xml`)

- `struts2.constantContributor` - Add custom Struts constants
- `struts2.resultContributor` - Custom result type path resolution
- `struts2.classContributor` - Extend class resolution (actions, interceptors)
- `struts2.paramNameCustomConverter` - Custom parameter name resolution

### Template Engine Integrations

Optional modules loaded via `<depends optional="true">`:

- `struts2-freemarker.xml` - FreeMarker template support
- `struts2-velocity.xml` - Velocity template support
- `struts2-spring.xml` - Spring integration
- `struts2-groovy.xml` - Groovy annotations support

### Test Organization

- `src/test/java` - Test classes following IntelliJ Platform test patterns
- `src/test/testData` - Test fixture files (XML, JSP, Java sources)
- Test base classes extend IntelliJ Platform's `LightJavaCodeInsightFixtureTestCase` or similar

### Generated Code

- `src/main/gen/` - Generated parser/lexer code (included in source set automatically)
- Source grammars: `src/main/grammar/ognl.bnf` (Grammar Kit), `_OgnlLexer.flex` (JFlex)

## IntelliJ Platform Version Mapping

| Branch | Platform Version | Build Range |
|--------|------------------|-------------|
| 242.x  | 2024.2           | 242.*       |
| 243.x  | 2024.3           | 243.*       |
| 251.x  | 2025.1           | 251.*       |
| 252.x  | 2025.2           | 252.*       |
| 253.x  | 2025.3           | 253.*       |

### Plugin Version Format

`{BRANCH}.{BUILD}.{FIX}` (e.g., `252.18970.1`)

- **BRANCH** - IntelliJ Platform branch (252 = 2025.2)
- **BUILD** - Automatically calculated in GitHub Actions as `18969 + git rev-list --count HEAD`
- **FIX** - Patch version (typically `1` for new builds)

The base value `18969` maintains historical continuity from when the plugin was donated by JetBrains to Apache Software
Foundation, ensuring version numbers continue from the previous build sequence.

## Platform Upgrade Checklist

When upgrading to a new IntelliJ Platform version:

1. **Update `gradle.properties`**:
  - `platformVersion` - Target platform (e.g., `2025.2`)
  - `pluginSinceBuild` / `pluginUntilBuild` - Build range (e.g., `251` to `252.*`)
  - `pluginVersion` - Match branch prefix (e.g., `252.x.y`)
2. **Check API Compatibility**:
  - Review https://jb.gg/intellij-api-changes for breaking changes
  - Run `./gradlew runPluginVerifier` to detect issues
  - Common changes: deprecated UI icons, removed internal APIs, changed test framework paths
3. **Update CI/Tooling** (if Java version changes):
  - `.github/workflows/build.yml` - Java version in setup
  - `qodana.yml` - Linter version and `projectJDK`
  - `build.gradle.kts` - `jvmToolchain()` version
4. **Fix Test Path Issues**:
  - IntelliJ Platform test frameworks sometimes change path resolution
  - Override `getBasePath()` and `getTestDataPath()` if tests fail to find fixtures
  - Use project-relative paths like `"src/test/testData/..."`
5. **Update CHANGELOG.md** with dependency upgrades

## Known Platform Quirks

**Test Data Path Resolution**: IntelliJ 2024.2+ changed how `LexerTestCase` and `DomStubTest` resolve paths. If tests
fail with "Cannot find source file", override path methods to use project-relative paths.

**Internal API Usage**: Some platform icons and utilities are internal. The plugin verifier will warn about these.
Prefer public APIs:

- Use `AllIcons.Nodes.*` instead of `PlatformIcons`
- Use `Charset.availableCharsets()` instead of `CharsetToolkit.getAvailableCharsets()`

**JSP Reference Providers**: The `javaee.web.customServletReferenceProvider` extension point behavior changed in 2024.2.
Action link references in JSP may need alternative registration approaches.

**Temporarily Disabled Tests**: Several tests are disabled pending 2025.3 compatibility fixes:
OgnlLexerTest, StrutsCompletionTest, StrutsHighlightingSpringTest, StrutsResultResolvingTest,
ActionLinkReferenceProviderTest. See CHANGELOG.md for full list - these should be re-enabled
when infrastructure issues are resolved.

## Claude Guidance

- **Always use https://jb.gg/intellij-api-changes** when researching IntelliJ Platform API changes
- **Update CHANGELOG.md** when upgrading dependencies: `- Dependencies - upgrade <name> to <version>`
- **Run `./gradlew test -x rat`** before committing to catch test regressions
- **Prefer editing existing files** over creating new ones
- When fixing test failures, check if path resolution changed before assuming code bugs