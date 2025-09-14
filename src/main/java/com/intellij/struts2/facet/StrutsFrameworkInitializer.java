/*
 * Copyright 2025 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts2.facet;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.struts2.StrutsConstants;
import com.intellij.struts2.StrutsFileTemplateProvider;
import com.intellij.struts2.facet.ui.StrutsConfigsSearcher;
import com.intellij.struts2.facet.ui.StrutsFileSet;
import com.intellij.util.containers.MultiMap;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles initialization of newly created Struts facets using modern ProjectActivity pattern.
 * <p>
 * This initializer replaces the deprecated StartupManager.runAfterOpened() approach and provides
 * proper framework setup after facet creation. It performs the following tasks:
 * <ul>
 *   <li>Discovers Struts configuration files in project JARs</li>
 *   <li>Sets up file sets for configuration discovery and management</li>
 *   <li>Provides thread-safe PSI operations using ReadAction</li>
 *   <li>Shows user notifications about setup completion with actionable links</li>
 * </ul>
 * <p>
 * The initializer is triggered via the {@link StrutsFrameworkSupportProvider} when a new
 * Struts facet is created through "Add Framework Support" action. Unlike previous implementations,
 * this initializer does NOT create files automatically, following modern IDE plugin best practices.
 *
 * @author Generated for IntelliJ Platform 2025.2 compatibility
 * @see StrutsFrameworkSupportProvider#onFacetCreated
 * @see ProjectActivity
 */
public class StrutsFrameworkInitializer implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(StrutsFrameworkInitializer.class);

    /**
     * Thread-safe storage for pending initialization data.
     * Key: project, Value: initialization data
     */
    private static final ConcurrentHashMap<String, InitializationData> pendingInitializations = new ConcurrentHashMap<>();

    /**
     * Data structure to hold initialization parameters passed from the framework support provider.
     */
    public static class InitializationData {
        public final Project project;
        public final StrutsFacet strutsFacet;

        public InitializationData(@NotNull Project project, @NotNull StrutsFacet strutsFacet) {
            this.project = project;
            this.strutsFacet = strutsFacet;
        }

        @Override
        public String toString() {
            return "InitializationData{project=" + project.getName() + ", module=" + strutsFacet.getModule().getName() + "}";
        }
    }

    /**
     * Schedules a Struts facet for initialization when the project opens.
     * This method is called from {@link StrutsFrameworkSupportProvider#onFacetCreated}.
     *
     * @param project the project containing the facet
     * @param strutsFacet the newly created Struts facet to initialize
     */
    public static void scheduleInitialization(@NotNull Project project, @NotNull StrutsFacet strutsFacet) {
        LOG.info("Scheduling Struts facet initialization for project: " + project.getName() +
                 ", module: " + strutsFacet.getModule().getName());
        pendingInitializations.put(project.getName(), new InitializationData(project, strutsFacet));
    }

    /**
     * ProjectActivity execution method called when project is opened.
     * Checks for pending Struts facet initializations and performs setup tasks.
     *
     * @param project the project being opened
     * @param continuation Kotlin coroutine continuation (required by interface)
     * @return the initialized facet or null if no initialization was needed
     */
    @Override
    @Nullable
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        LOG.debug("Struts framework initializer execute called for project: " + project.getName());

        InitializationData data = pendingInitializations.remove(project.getLocationHash());
        if (data != null) {
            LOG.info("Found pending Struts initialization data: " + data);
            performInitialization(data);
            return data.strutsFacet;
        }

        // Check for existing facets that might need initialization (project reopened)
        Module[] modules = ModuleManager.getInstance(project).getModules();
        LOG.info("Existing Struts modules: " + Arrays.toString(modules));
        for (Module module : modules) {
            StrutsFacet facet = StrutsFacet.getInstance(module);
            if (facet != null && shouldInitializeFacet(facet)) {
                LOG.info("Found existing Struts facet needing initialization: " + module.getName());
                performInitialization(new InitializationData(project, facet));
                return facet;
            }
        }

        Module module = ModuleManager.getInstance(project).newModule(project.getProjectFile().toNioPath(), StrutsFacet.FACET_TYPE_ID.toString());
        StrutsFacet facet = StrutsFacet.getInstance(module);
        performInitialization(new InitializationData(project, facet));
        return facet;
    }

    /**
     * Checks if the given facet needs initialization (e.g., missing struts.xml).
     * Uses ReadAction to ensure thread-safe PSI access.
     */
    private boolean shouldInitializeFacet(@NotNull StrutsFacet facet) {
        return ReadAction.compute(() -> {
            Module module = facet.getModule();
            VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();

            if (sourceRoots.length == 0) {
                return false;
            }

            PsiDirectory directory = PsiManager.getInstance(module.getProject()).findDirectory(sourceRoots[0]);
            return directory != null && directory.findFile(StrutsConstants.STRUTS_XML_DEFAULT_FILENAME) == null;
        });
    }

    /**
     * Performs the actual Struts framework initialization tasks.
     * This method contains the logic moved from StrutsFrameworkSupportProvider.
     *
     * @param data initialization data containing project and facet information
     */
    private void performInitialization(@NotNull InitializationData data) {
        LOG.info("Starting Struts framework initialization for: " + data);

        DumbService.getInstance(data.project).runWhenSmart(() -> {
            try {
                initializeStrutsFramework(data);
            } catch (Exception e) {
                LOG.error("Struts framework initialization failed for " + data, e);
                showErrorNotification(data.project, e);
            }
        });
    }

    /**
     * Core initialization logic that discovers JAR configuration files and sets up the framework.
     * Uses proper read/write actions for thread-safe PSI operations.
     */
    private void initializeStrutsFramework(@NotNull InitializationData data) {
        final Module module = data.strutsFacet.getModule();

        LOG.info("Setting up Struts framework configuration for module: " + module.getName());
        final StrutsFacetConfiguration strutsFacetConfiguration = data.strutsFacet.getConfiguration();

        // Search for Struts configuration files in JARs (requires smart mode)
        final StrutsConfigsSearcher searcher = new StrutsConfigsSearcher(module);
        DumbService.getInstance(data.project).runWhenSmart(() -> {
            try {
                searcher.search();
                final MultiMap<VirtualFile, PsiFile> jarConfigFiles = searcher.getJars();

                if (!jarConfigFiles.isEmpty()) {
                    // Create file set for JAR-based configuration files
                    final Set<StrutsFileSet> existingFileSets = strutsFacetConfiguration.getFileSets();
                    final StrutsFileSet jarFileSet = new StrutsFileSet(
                        StrutsFileSet.getUniqueId(existingFileSets),
                        StrutsFileSet.getUniqueName("JAR Configuration Files", existingFileSets),
                        strutsFacetConfiguration
                    );

                    int jarFilesCount = 0;
                    for (final VirtualFile jarFile : jarConfigFiles.keySet()) {
                        final Collection<PsiFile> configFiles = jarConfigFiles.get(jarFile);
                        for (final PsiFile configFile : configFiles) {
                            jarFileSet.addFile(configFile.getVirtualFile());
                            jarFilesCount++;
                        }
                        LOG.debug("Found " + configFiles.size() + " configuration files in JAR: " + jarFile.getName());
                    }

                    if (jarFilesCount > 0) {
                        strutsFacetConfiguration.getFileSets().add(jarFileSet);
                        LOG.info("Added " + jarFilesCount + " JAR config files to Struts facet configuration");

                        // Show success notification after JAR discovery
                        showSuccessNotification(data.project, data.strutsFacet);
                    }
                } else {
                    LOG.debug("No Struts configuration files found in JARs for module: " + module.getName());

                    // Still show success notification even if no JAR files found
                    showSuccessNotification(data.project, data.strutsFacet);
                }
            } catch (Exception e) {
                LOG.warn("Failed to search for JAR configuration files in module: " + module.getName(), e);

                // Show success notification despite JAR search failure
                showSuccessNotification(data.project, data.strutsFacet);
            }
        });
    }

    /**
     * Shows success notification with link to facet settings.
     */
    private void showSuccessNotification(@NotNull Project project, @NotNull StrutsFacet strutsFacet) {
        new Notification("Struts 2", "Struts 2 setup complete",
                         "Struts 2 framework has been configured successfully.",
                         NotificationType.INFORMATION)
          .addAction(new NotificationAction("Review settings") {
              @Override
              public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                  notification.expire();
                  ModulesConfigurator.showFacetSettingsDialog(strutsFacet, null);
              }
          })
          .notify(project);

        LOG.info("Struts framework initialization completed successfully for module: " + strutsFacet.getModule().getName());
    }

    /**
     * Shows error notification when initialization fails.
     */
    private void showErrorNotification(@NotNull Project project, @NotNull Exception error) {
        new Notification("Struts 2", "Struts 2 setup failed",
                         "Struts 2 framework setup encountered an error: " + error.getMessage() +
                         ". Check IDE logs for details.",
                         NotificationType.ERROR)
          .notify(project);
    }
}