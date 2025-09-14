/*
 * Copyright 2016 The authors
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

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.framework.library.DownloadableLibraryService;
import com.intellij.framework.library.FrameworkSupportWithLibrary;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurableBase;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProviderBase;
import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * "Add Framework" support.
 *
 * @author Yann C&eacute;bron
 */
public class StrutsFrameworkSupportProvider extends FacetBasedFrameworkSupportProvider<StrutsFacet> {

  private static final Logger LOG = Logger.getInstance(StrutsFrameworkSupportProvider.class);

  public StrutsFrameworkSupportProvider() {
    super(StrutsFacetType.getInstance());
  }

  @Override
  public String getTitle() {
    return UIUtil.replaceMnemonicAmpersand("Struts &2");
  }

  @NotNull
  @Override
  public FrameworkSupportConfigurableBase createConfigurable(@NotNull final FrameworkSupportModel model) {
    return new Struts2FrameworkSupportConfigurable(this, model, getVersions(), getVersionLabelText());
  }

  @Override
  protected void setupConfiguration(final StrutsFacet strutsFacet,
                                    final ModifiableRootModel modifiableRootModel, final FrameworkVersion version) {
  }

  @Override
  public boolean isEnabledForModuleBuilder(@NotNull ModuleBuilder builder) {
    return false;
  }

  @Override
  protected void onFacetCreated(final StrutsFacet strutsFacet,
                                final ModifiableRootModel modifiableRootModel,
                                final FrameworkVersion version) {
    LOG.info("onFacetCreated: " + strutsFacet);

    // Schedule initialization using modern ProjectActivity pattern
    // The StrutsFrameworkInitializer will handle the actual setup when the project opens
    StrutsFrameworkInitializer.scheduleInitialization(modifiableRootModel.getProject(), strutsFacet);
  }

  private static final class Struts2FrameworkSupportConfigurable extends FrameworkSupportConfigurableBase
    implements FrameworkSupportWithLibrary {

    private Struts2FrameworkSupportConfigurable(FrameworkSupportProviderBase frameworkSupportProvider,
                                                FrameworkSupportModel model,
                                                @NotNull List<FrameworkVersion> versions,
                                                @Nullable String versionLabelText) {
      super(frameworkSupportProvider, model, versions, versionLabelText);
    }

    @NotNull
    @Override
    public CustomLibraryDescription createLibraryDescription() {
      return DownloadableLibraryService.getInstance().createDescriptionForType(Struts2LibraryType.class);
    }

    @Override
    public boolean isLibraryOnly() {
      return false;
    }
  }
}
