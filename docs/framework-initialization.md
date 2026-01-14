# IntelliJ Platform Framework Initialization Guide

This guide provides comprehensive documentation on how to initialize frameworks in IntelliJ Platform plugins, based on analysis of both official documentation and the Apache Struts plugin implementation.

## Table of Contents

1. [Modern Framework Initialization (2025)](#modern-framework-initialization-2025)
2. [Complete Framework Integration Architecture](#complete-framework-integration-architecture)
3. [Implementation Patterns](#implementation-patterns)
4. [Best Practices](#best-practices)
5. [Common Issues and Solutions](#common-issues-and-solutions)
6. [Migration from Legacy Approaches](#migration-from-legacy-approaches)

## Modern Framework Initialization (2025)

### ProjectActivity - The Current Standard

IntelliJ Platform 2024.2+ uses `ProjectActivity` as the modern replacement for deprecated initialization patterns.

**Key Requirements:**
- Must be implemented with **Kotlin coroutines** (Java not supported for new implementations)
- Register via `<postStartupActivity implementation="..."/>` extension point
- Executes after project is opened with proper coroutine context

**Example Registration:**
```xml
<extensions defaultExtensionNs="com.intellij">
  <postStartupActivity implementation="com.example.MyFrameworkInitializer"/>
</extensions>
```

### Deprecated Patterns (Avoid These)

- ❌ **Components**: Deprecated, don't support dynamic loading
- ❌ **StartupActivity**: Marked as `@Obsolete`
- ❌ **StartupManager.runAfterOpened()**: Internal API, removed

## Complete Framework Integration Architecture

The Apache Struts plugin demonstrates the comprehensive pattern for framework integration. Here are the core components:

### 1. FacetType

Defines the framework facet with metadata and capabilities.

```java
public class StrutsFacetType extends FacetType<StrutsFacet, StrutsFacetConfiguration> {

  StrutsFacetType() {
    super(StrutsFacet.FACET_TYPE_ID, "Struts2", "Struts 2");
  }

  @Override
  public boolean isSuitableModuleType(final ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;  // Restrict to Java modules
  }

  @Override
  public Icon getIcon() {
    return Struts2Icons.Action;
  }
}
```

**Key Features:**
- Unique facet type ID
- Module type restrictions
- Icon and help topic configuration
- Factory methods for facet and configuration creation

### 2. FacetConfiguration

Manages framework-specific settings and persistence.

```java
public class StrutsFacetConfiguration extends SimpleModificationTracker
    implements FacetConfiguration, ModificationTracker, Disposable {

  @Override
  public FacetEditorTab[] createEditorTabs(final FacetEditorContext editorContext,
                                           final FacetValidatorsManager validatorsManager) {
    return new FacetEditorTab[]{
      new FileSetConfigurationTab(this, editorContext),
      new FeaturesConfigurationTab(this)
    };
  }

  @Override
  public void readExternal(final Element element) throws InvalidDataException {
    // XML persistence logic
  }

  @Override
  public void writeExternal(final Element element) throws WriteExternalException {
    // XML persistence logic
  }
}
```

**Responsibilities:**
- Settings persistence (XML serialization)
- Editor tabs for configuration UI
- Validation and modification tracking
- Resource disposal

### 3. Facet

The main facet class providing framework access at module level.

```java
public class StrutsFacet extends Facet<StrutsFacetConfiguration> {

  public static final FacetTypeId<StrutsFacet> FACET_TYPE_ID = new FacetTypeId<>("struts2");

  @Nullable
  public static StrutsFacet getInstance(@NotNull final Module module) {
    return FacetManager.getInstance(module).getFacetByType(FACET_TYPE_ID);
  }

  @Nullable
  public WebFacet getWebFacet() {
    return FacetManager.getInstance(getModule()).getFacetByType(WebFacet.ID);
  }
}
```

**Key Features:**
- Static access methods for convenience
- Integration with other facets (e.g., WebFacet)
- Module-scoped framework instance

### 4. FrameworkDetector

Automatically detects framework presence in projects.

```java
public class StrutsFrameworkDetector extends FacetBasedFrameworkDetector<StrutsFacet, StrutsFacetConfiguration> {

  @Override
  public ElementPattern<FileContent> createSuitableFilePattern() {
    return FileContentPattern.fileContent()
      .withName(StrutsConstants.STRUTS_XML_DEFAULT_FILENAME)  // "struts.xml"
      .xmlWithRootTag(StrutsRoot.TAG_NAME);                   // <struts>
  }

  @Override
  public boolean isSuitableUnderlyingFacetConfiguration(final FacetConfiguration underlying,
                                                        final StrutsFacetConfiguration configuration,
                                                        final Set<? extends VirtualFile> files) {
    return underlying instanceof WebFacetConfiguration;  // Requires Web facet
  }
}
```

**Detection Strategy:**
- File pattern matching (e.g., struts.xml with &lt;struts&gt; root)
- Underlying facet validation
- Multi-file detection support

### 5. FrameworkSupportProvider

Handles "Add Framework Support" dialog integration.

```java
public class StrutsFrameworkSupportProvider extends FacetBasedFrameworkSupportProvider<StrutsFacet> {

  @Override
  public String getTitle() {
    return UIUtil.replaceMnemonicAmpersand("Struts &2");
  }

  @Override
  protected void onFacetCreated(final StrutsFacet strutsFacet,
                                final ModifiableRootModel modifiableRootModel,
                                final FrameworkVersion version) {
    // Trigger initialization after facet creation
  }
}
```

**Capabilities:**
- Framework library management
- Version selection UI
- Integration with project setup wizard
- Post-creation initialization trigger

### 6. Framework Initializer

Performs complex setup tasks after framework addition.

```java
public class StrutsFrameworkInitializer implements ProjectActivity {

  @Override
  public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    DumbService.getInstance(project).runWhenSmart(() -> {
      // Create default configuration files
      final FileTemplate strutsXmlTemplate = templateProvider.determineFileTemplate(project);
      final PsiElement strutsXml = FileTemplateUtil.createFromTemplate(strutsXmlTemplate, ...);

      // Configure web.xml if present
      WriteCommandAction.writeCommandAction(project).run(() -> {
        // Add filters and mappings
      });

      // Show completion notification
      new Notification("Framework Setup", "Framework has been configured successfully", ...)
        .notify(project);
    });
  }
}
```

**Initialization Tasks:**
- Create default configuration files (struts.xml)
- Configure web.xml with filters/mappings
- Set up file sets for configuration discovery
- Show user notifications
- Handle error scenarios gracefully

## Implementation Patterns

### Plugin.xml Registration

```xml
<extensions defaultExtensionNs="com.intellij">
  <!-- Core Framework Components -->
  <facetType implementation="com.example.MyFacetType"/>
  <frameworkSupport implementation="com.example.MyFrameworkSupportProvider"/>
  <framework.detector implementation="com.example.MyFrameworkDetector"/>
  <library.type implementation="com.example.MyLibraryType"/>

  <!-- Modern Initialization -->
  <postStartupActivity implementation="com.example.MyFrameworkInitializer"/>
</extensions>
```

### Initialization Sequence Flow

1. **Detection Phase**: `FrameworkDetector` scans project files
2. **Support Addition**: User adds framework via "Add Framework Support"
3. **Facet Creation**: `FrameworkSupportProvider` creates facet and configuration
4. **Initialization**: `ProjectActivity` performs setup tasks
5. **User Notification**: Success/failure feedback shown

### Integration with IntelliJ Services

```java
// Index-dependent operations
DumbService.getInstance(project).runWhenSmart(() -> {
  // Search, analysis, file creation
});

// Write operations
WriteCommandAction.writeCommandAction(project).run(() -> {
  // File modifications
});

// File template usage
FileTemplate template = FileTemplateManager.getInstance(project)
  .getInternalTemplate("framework-config.xml");
PsiElement created = FileTemplateUtil.createFromTemplate(template, ...);
```

## Best Practices

### 1. Robust Error Handling

```java
try {
  // Framework initialization logic
} catch (Exception e) {
  LOG.error("Framework initialization failed", e);

  // Show user-friendly error notification
  Notifications.Bus.notify(new Notification(
    "Framework Setup",
    "Setup Failed",
    "Framework setup encountered an error: " + e.getMessage(),
    NotificationType.ERROR
  ), project);
}
```

### 2. User Communication

```java
// Success notification with actionable links
NotificationListener showSettingsListener = (notification, event) -> {
  if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
    notification.expire();
    ModulesConfigurator.showFacetSettingsDialog(facet, null);
  }
};

new Notification("Framework Setup", "Setup Complete",
  "Framework configured successfully. <a href=\"settings\">Review settings</a>",
  NotificationType.INFORMATION)
  .setListener(showSettingsListener)
  .notify(project);
```

### 3. Incremental Initialization

```java
// Check if already initialized
if (existingConfigFile != null) {
  LOG.info("Framework already configured, skipping initialization");
  return;
}

// Verify prerequisites
if (requiredFacet == null) {
  LOG.warn("Required underlying facet not found, postponing initialization");
  return;
}
```

### 4. Resource Management

```java
public class MyFacetConfiguration implements FacetConfiguration, Disposable {

  @Override
  public void dispose() {
    // Clean up resources, listeners, etc.
  }
}

// Register for disposal
Disposer.register(facet, configuration);
```

## Common Issues and Solutions

### Issue: ProjectActivity Not Executing

**Symptoms:** Framework initializer never runs
**Cause:** Missing plugin.xml registration or incorrect extension point
**Solution:**
```xml
<postStartupActivity implementation="com.example.MyFrameworkInitializer"/>
```

### Issue: Dumb Mode Errors

**Symptoms:** Index-related exceptions during initialization
**Cause:** Accessing indices while IntelliJ is indexing
**Solution:**
```java
DumbService.getInstance(project).runWhenSmart(() -> {
  // Index-dependent logic here
});
```

### Issue: Framework Not Auto-Detected

**Symptoms:** Framework detector doesn't trigger
**Cause:** Incorrect file pattern or missing registration
**Solution:**
```java
@Override
public ElementPattern<FileContent> createSuitableFilePattern() {
  return FileContentPattern.fileContent()
    .withName("framework-config.xml")  // Exact filename
    .xmlWithRootTag("framework");      // XML root element
}
```

### Issue: Facet Creation Fails

**Symptoms:** "Add Framework Support" fails silently
**Cause:** Unsuitable module type or missing underlying facets
**Solution:**
```java
@Override
public boolean isSuitableModuleType(final ModuleType moduleType) {
  return moduleType instanceof JavaModuleType;  // Be specific
}

@Override
public boolean isSuitableUnderlyingFacetConfiguration(...) {
  return underlying instanceof RequiredFacetConfiguration;
}
```

## Migration from Legacy Approaches

### From StartupActivity to ProjectActivity

**Legacy (Deprecated):**
```java
public class OldInitializer implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    // Old initialization logic
  }
}
```

**Modern (Recommended):**
```java
// Kotlin implementation required for new code
class ModernInitializer : ProjectActivity {
  override suspend fun execute(project: Project) {
    withContext(Dispatchers.IO) {
      // Initialization logic with coroutines
    }
  }
}
```

### From Components to Services

**Legacy (Deprecated):**
```xml
<project-components>
  <component>
    <implementation-class>com.example.ProjectComponent</implementation-class>
  </component>
</project-components>
```

**Modern (Recommended):**
```xml
<extensions defaultExtensionNs="com.intellij">
  <projectService serviceImplementation="com.example.ProjectService"/>
</extensions>
```

## Conclusion

Framework initialization in modern IntelliJ Platform plugins requires:

1. **Complete Architecture**: Facet + Configuration + Detector + Support Provider + Initializer
2. **Modern APIs**: ProjectActivity with Kotlin coroutines (for new implementations)
3. **Proper Registration**: All components registered in plugin.xml
4. **Robust Implementation**: Error handling, user communication, resource management
5. **Testing**: Verification across different project configurations

The Apache Struts plugin provides an excellent reference implementation, though it still uses Java-based ProjectActivity which is acceptable for existing code but should be migrated to Kotlin for new implementations.

For questions or issues, consult:
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/)
- [API Changes List](https://jb.gg/intellij-api-changes)
- [Facet Documentation](https://plugins.jetbrains.com/docs/intellij/facet.html)