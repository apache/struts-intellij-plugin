/*
 * Copyright 2011 The authors
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

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.javaee.web.facet.WebFacetConfiguration;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.struts2.StrutsConstants;
import com.intellij.struts2.dom.struts.StrutsRoot;
import com.intellij.struts2.facet.ui.StrutsConfigsSearcher;
import com.intellij.struts2.facet.ui.StrutsFileSet;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yann C&eacute;bron
 */
public class StrutsFrameworkDetector extends FacetBasedFrameworkDetector<StrutsFacet, StrutsFacetConfiguration> {

  private static final Logger LOG = Logger.getInstance(StrutsFrameworkDetector.class);

  public StrutsFrameworkDetector() {
    super("struts2");
  }

  @Override
  public void setupFacet(@NotNull StrutsFacet facet, ModifiableRootModel model) {
    LOG.info("Setting up Struts facet for module: " + facet.getModule().getName());

    com.intellij.openapi.module.Module module = facet.getModule();
    StrutsConfigsSearcher searcher = new StrutsConfigsSearcher(module);

    // Use parent search() method which properly separates module files from JAR files
    searcher.search();

    // Use URL-based deduplication (LinkedHashMap preserves insertion order)
    // Key: file URL, Value: VirtualFile - ensures each unique URL is only added once
    Map<String, VirtualFile> configFilesByUrl = new LinkedHashMap<>();

    // Add module configuration files
    MultiMap<com.intellij.openapi.module.Module, com.intellij.psi.PsiFile> moduleFiles = searcher.getFilesByModules();
    int moduleFilesCount = 0;
    for (com.intellij.openapi.module.Module mod : moduleFiles.keySet()) {
      for (com.intellij.psi.PsiFile psiFile : moduleFiles.get(mod)) {
        VirtualFile vf = psiFile.getVirtualFile();
        if (vf != null) {
          String url = vf.getUrl();
          if (!configFilesByUrl.containsKey(url)) {
            configFilesByUrl.put(url, vf);
            moduleFilesCount++;
          } else {
            LOG.debug("Skipping duplicate module file: " + url);
          }
        }
      }
    }

    // Add JAR configuration files (framework JARs like struts2-core)
    MultiMap<VirtualFile, com.intellij.psi.PsiFile> jarConfigFiles = searcher.getJars();
    int jarFilesCount = 0;
    for (final VirtualFile jarFile : jarConfigFiles.keySet()) {
      final Collection<com.intellij.psi.PsiFile> jarPsiFiles = jarConfigFiles.get(jarFile);
      for (final com.intellij.psi.PsiFile psiFile : jarPsiFiles) {
        VirtualFile vf = psiFile.getVirtualFile();
        if (vf != null) {
          String url = vf.getUrl();
          if (!configFilesByUrl.containsKey(url)) {
            configFilesByUrl.put(url, vf);
            jarFilesCount++;
          } else {
            LOG.debug("Skipping duplicate JAR file: " + url);
          }
        }
      }
      LOG.debug("Found " + jarPsiFiles.size() + " configuration files in JAR: " + jarFile.getName());
    }

    List<VirtualFile> configFiles = new ArrayList<>(configFilesByUrl.values());
    LOG.info("Found " + configFiles.size() + " unique Struts configuration files (" + moduleFilesCount + " from modules, " + jarFilesCount + " from JARs)");

    if (!configFiles.isEmpty()) {
      StrutsFacetConfiguration config = facet.getConfiguration();
      Set<StrutsFileSet> fileSets = config.getFileSets();

      StrutsFileSet fileSet = new StrutsFileSet(
        StrutsFileSet.getUniqueId(fileSets),
        StrutsFileSet.getUniqueName("Detected Configuration", fileSets),
        config
      );

      for (VirtualFile file : configFiles) {
        fileSet.addFile(file);
        LOG.debug("Added detected file to file set: " + file.getPath());
      }

      fileSets.add(fileSet);
      LOG.info("Created file set with " + configFiles.size() + " files for module: " + facet.getModule().getName());

      // Show notification with discovered files summary
      showSetupCompleteNotification(facet, configFiles, moduleFilesCount, jarFilesCount);
    }
  }

  /**
   * Shows a notification after Struts framework setup is complete, listing all discovered configuration files.
   * This helps users understand what files were found, especially JAR files that weren't visible in the
   * Setup Frameworks dialog.
   */
  private void showSetupCompleteNotification(@NotNull StrutsFacet facet,
                                             @NotNull List<VirtualFile> configFiles,
                                             int moduleFilesCount,
                                             int jarFilesCount) {
    StringBuilder content = new StringBuilder();
    content.append("Configured ").append(configFiles.size()).append(" file");
    if (configFiles.size() != 1) content.append("s");
    content.append(" (").append(moduleFilesCount).append(" from project");
    if (jarFilesCount > 0) {
      content.append(", ").append(jarFilesCount).append(" from JARs");
    }
    content.append("):<br/>");

    // List module files first
    for (VirtualFile file : configFiles) {
      String path = file.getPath();
      if (!path.contains(".jar!")) {
        content.append("• ").append(file.getName()).append("<br/>");
      }
    }

    // Then list JAR files
    if (jarFilesCount > 0) {
      content.append("<br/><b>From JARs:</b><br/>");
      for (VirtualFile file : configFiles) {
        String path = file.getPath();
        if (path.contains(".jar!")) {
          // Extract just the file name and JAR name for brevity
          String jarName = extractJarName(path);
          content.append("• ").append(file.getName());
          if (jarName != null) {
            content.append(" <i>(").append(jarName).append(")</i>");
          }
          content.append("<br/>");
        }
      }
    }

    new Notification("Struts 2", "Struts 2 framework configured", content.toString(), NotificationType.INFORMATION)
      .addAction(new NotificationAction("Review settings") {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
          notification.expire();
          ModulesConfigurator.showFacetSettingsDialog(facet, null);
        }
      })
      .notify(facet.getModule().getProject());
  }

  /**
   * Extracts the JAR file name from a path like "/path/to/struts2-core-7.1.1.jar!/struts-default.xml"
   */
  private String extractJarName(String path) {
    int jarIndex = path.lastIndexOf(".jar!");
    if (jarIndex > 0) {
      int startIndex = path.lastIndexOf('/', jarIndex);
      if (startIndex >= 0) {
        return path.substring(startIndex + 1, jarIndex + 4); // +4 to include ".jar"
      }
    }
    return null;
  }

  @NotNull
  @Override
  public FacetType<StrutsFacet, StrutsFacetConfiguration> getFacetType() {
    return StrutsFacetType.getInstance();
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return XmlFileType.INSTANCE;
  }

  @NotNull
  @Override
  public ElementPattern<FileContent> createSuitableFilePattern() {
    return FileContentPattern.fileContent()
      .withName(StrutsConstants.STRUTS_XML_DEFAULT_FILENAME)
      .xmlWithRootTag(StrutsRoot.TAG_NAME);
  }

  @Override
  public boolean isSuitableUnderlyingFacetConfiguration(final FacetConfiguration underlying,
                                                        final StrutsFacetConfiguration configuration,
                                                        final Set<? extends VirtualFile> files) {
    // Check if the underlying configuration is a WebFacetConfiguration
    return underlying instanceof WebFacetConfiguration;
  }
}
