/*
 * Copyright 2015 The authors
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
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.libraries.FacetLibrariesValidator;
import com.intellij.facet.ui.libraries.LibraryInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.struts2.facet.ui.FeaturesConfigurationTab;
import com.intellij.struts2.facet.ui.FileSetConfigurationTab;
import com.intellij.struts2.facet.ui.StrutsFileSet;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides the configuration tabs and reads/stores settings.
 *
 * @author Yann C&eacute;bron
 */
public class StrutsFacetConfiguration extends SimpleModificationTracker
    implements FacetConfiguration, PersistentStateComponent<Element>, Disposable {

  // Filesets
  @NonNls
  private static final String FILESET = "fileset";
  @NonNls
  private static final String SET_ID = "id";
  @NonNls
  private static final String SET_NAME = "name";
  @NonNls
  private static final String SET_REMOVED = "removed";
  @NonNls
  private static final String FILE = "file";


  /**
   * Settings for {@link FileSetConfigurationTab}.
   */
  private final Set<StrutsFileSet> myFileSets = new LinkedHashSet<>();


  // Features - tab
  private static final String PROPERTIES_KEYS = "propertiesKeys";
  private static final String PROPERTIES_KEYS_DISABLED = "disabled";

  private boolean myPropertiesKeysDisabled = false;

  /**
   * Gets the currently configured filesets.
   *
   * @return Filesets.
   */
  public Set<StrutsFileSet> getFileSets() {
    return myFileSets;
  }

  public boolean isPropertiesKeysDisabled() {
    return myPropertiesKeysDisabled;
  }

  public void setPropertiesKeysDisabled(final boolean myPropertiesKeysDisabled) {
    this.myPropertiesKeysDisabled = myPropertiesKeysDisabled;
  }

  @Override
  public FacetEditorTab[] createEditorTabs(final FacetEditorContext editorContext,
                                           final FacetValidatorsManager validatorsManager) {
    final FacetLibrariesValidator validator =
      FacetEditorsFactory.getInstance().createLibrariesValidator(LibraryInfo.EMPTY_ARRAY,
                                                                 new StrutsFacetLibrariesValidatorDescription(),
                                                                 editorContext,
                                                                 validatorsManager);
    validatorsManager.registerValidator(validator);

    return new FacetEditorTab[]{new FileSetConfigurationTab(this, editorContext),
                                new FeaturesConfigurationTab(this)};
  }

  @Override
  public @Nullable Element getState() {
    final Element element = new Element("state");

    for (final StrutsFileSet fileSet : myFileSets) {
      final Element setElement = new Element(FILESET);
      setElement.setAttribute(SET_ID, fileSet.getId());
      setElement.setAttribute(SET_NAME, fileSet.getName());
      setElement.setAttribute(SET_REMOVED, Boolean.toString(fileSet.isRemoved()));
      element.addContent(setElement);

      for (final VirtualFilePointer fileName : fileSet.getFiles()) {
        final Element fileElement = new Element(FILE);
        fileElement.setText(fileName.getUrl());
        setElement.addContent(fileElement);
      }
    }

    final Element propertiesElement = new Element(PROPERTIES_KEYS);
    propertiesElement.setAttribute(PROPERTIES_KEYS_DISABLED, Boolean.toString(myPropertiesKeysDisabled));
    element.addContent(propertiesElement);

    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    myFileSets.clear();
    myPropertiesKeysDisabled = false;

    for (final Element setElement : state.getChildren(FILESET)) {
      final String setName = setElement.getAttributeValue(SET_NAME);
      final String setId = setElement.getAttributeValue(SET_ID);
      final String removed = setElement.getAttributeValue(SET_REMOVED);
      if (setName != null && setId != null) {
        final StrutsFileSet fileSet = new StrutsFileSet(setId, setName, this);
        final List<Element> files = setElement.getChildren(FILE);
        for (final Element fileElement : files) {
          final String text = fileElement.getText();
          fileSet.addFile(text);
        }
        fileSet.setRemoved(Boolean.parseBoolean(removed));
        myFileSets.add(fileSet);
      }
    }

    final Element propertiesElement = state.getChild(PROPERTIES_KEYS);
    if (propertiesElement != null) {
      final String disabled = propertiesElement.getAttributeValue(PROPERTIES_KEYS_DISABLED);
      myPropertiesKeysDisabled = Boolean.parseBoolean(disabled);
    }
  }

  public void setModified() {
    incModificationCount();
  }

  @Override
  public void dispose() {
  }
}