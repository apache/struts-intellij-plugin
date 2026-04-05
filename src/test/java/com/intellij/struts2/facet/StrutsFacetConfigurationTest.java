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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.facet.ui.StrutsFileSet;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link StrutsFacetConfiguration} persistence via {@code PersistentStateComponent<Element>}.
 */
public class StrutsFacetConfigurationTest extends BasicLightHighlightingTestCase {

  private final List<Disposable> myDisposables = new ArrayList<>();

  @Override
  @NotNull
  protected String getTestDataLocation() {
    return "";
  }

  @Override
  protected void performTearDown() {
    for (Disposable d : myDisposables) {
      Disposer.dispose(d);
    }
    myDisposables.clear();
  }

  private StrutsFacetConfiguration createDisposableConfig() {
    StrutsFacetConfiguration config = new StrutsFacetConfiguration();
    myDisposables.add(config);
    return config;
  }

  private StrutsFileSet createTrackedFileSet(String id, String name, StrutsFacetConfiguration parent) {
    StrutsFileSet fileSet = new StrutsFileSet(id, name, parent);
    myDisposables.add(fileSet);
    return fileSet;
  }

  public void testRoundTripPreservesFileSets() {
    final StrutsFacet facet = StrutsFacet.getInstance(getModule());
    assertNotNull(facet);
    final StrutsFacetConfiguration config = facet.getConfiguration();

    final StrutsFileSet fileSet = createTrackedFileSet("s2fileset1", "My Struts Config", config);
    fileSet.addFile("file:///project/src/main/resources/struts.xml");
    fileSet.addFile("file:///project/src/main/resources/struts-admin.xml");
    config.getFileSets().add(fileSet);

    final StrutsFileSet removedSet = createTrackedFileSet("s2fileset2", "Removed Set", config);
    removedSet.setRemoved(true);
    removedSet.addFile("file:///project/src/main/resources/old-struts.xml");
    config.getFileSets().add(removedSet);

    final Element state = config.getState();
    assertNotNull(state);

    final StrutsFacetConfiguration loaded = createDisposableConfig();
    loaded.loadState(state);

    final Set<StrutsFileSet> loadedSets = loaded.getFileSets();
    assertEquals(2, loadedSets.size());

    final List<StrutsFileSet> setList = new ArrayList<>(loadedSets);

    assertEquals("s2fileset1", setList.get(0).getId());
    assertEquals("My Struts Config", setList.get(0).getName());
    assertFalse(setList.get(0).isRemoved());
    assertEquals(2, setList.get(0).getFiles().size());
    assertEquals("file:///project/src/main/resources/struts.xml", setList.get(0).getFiles().get(0).getUrl());
    assertEquals("file:///project/src/main/resources/struts-admin.xml", setList.get(0).getFiles().get(1).getUrl());

    assertEquals("s2fileset2", setList.get(1).getId());
    assertEquals("Removed Set", setList.get(1).getName());
    assertTrue(setList.get(1).isRemoved());
    assertEquals(1, setList.get(1).getFiles().size());
  }

  public void testRoundTripPreservesPropertiesKeysDisabled() {
    final StrutsFacet facet = StrutsFacet.getInstance(getModule());
    assertNotNull(facet);
    final StrutsFacetConfiguration config = facet.getConfiguration();
    config.setPropertiesKeysDisabled(true);

    final Element state = config.getState();
    assertNotNull(state);

    final StrutsFacetConfiguration loaded = createDisposableConfig();
    loaded.loadState(state);
    assertTrue(loaded.isPropertiesKeysDisabled());
  }

  public void testDefaultStateHasPropertiesKeysEnabled() {
    final StrutsFacetConfiguration config = createDisposableConfig();

    final Element emptyState = new Element("state");
    config.loadState(emptyState);

    assertFalse(config.isPropertiesKeysDisabled());
    assertTrue(config.getFileSets().isEmpty());
  }

  public void testMalformedFileSetWithoutIdIsSkipped() {
    final Element state = new Element("state");
    final Element badFileSet = new Element("fileset");
    badFileSet.setAttribute("name", "Has Name");
    state.addContent(badFileSet);

    final StrutsFacetConfiguration config = createDisposableConfig();
    config.loadState(state);

    assertTrue(config.getFileSets().isEmpty());
  }

  public void testMalformedFileSetWithoutNameIsSkipped() {
    final Element state = new Element("state");
    final Element badFileSet = new Element("fileset");
    badFileSet.setAttribute("id", "s2fileset1");
    state.addContent(badFileSet);

    final StrutsFacetConfiguration config = createDisposableConfig();
    config.loadState(state);

    assertTrue(config.getFileSets().isEmpty());
  }

  public void testLoadStateClearsPreviousState() {
    final StrutsFacetConfiguration config = createDisposableConfig();

    final Element initialState = new Element("state");
    final Element fileSetElement = new Element("fileset");
    fileSetElement.setAttribute("id", "s2fileset1");
    fileSetElement.setAttribute("name", "First");
    fileSetElement.setAttribute("removed", "false");
    initialState.addContent(fileSetElement);

    final Element propsElement = new Element("propertiesKeys");
    propsElement.setAttribute("disabled", "true");
    initialState.addContent(propsElement);

    config.loadState(initialState);
    assertEquals(1, config.getFileSets().size());
    assertTrue(config.isPropertiesKeysDisabled());

    config.loadState(new Element("state"));

    assertTrue(config.getFileSets().isEmpty());
    assertFalse(config.isPropertiesKeysDisabled());
  }

  public void testXmlStructureBackwardCompatibility() {
    final StrutsFacet facet = StrutsFacet.getInstance(getModule());
    assertNotNull(facet);
    final StrutsFacetConfiguration config = facet.getConfiguration();

    final StrutsFileSet fileSet = createTrackedFileSet("s2fileset1", "Test", config);
    fileSet.addFile("file:///test/struts.xml");
    fileSet.setRemoved(true);
    config.getFileSets().add(fileSet);
    config.setPropertiesKeysDisabled(true);

    final Element state = config.getState();
    assertNotNull(state);

    final List<Element> filesets = state.getChildren("fileset");
    assertEquals(1, filesets.size());
    assertEquals("s2fileset1", filesets.get(0).getAttributeValue("id"));
    assertEquals("Test", filesets.get(0).getAttributeValue("name"));
    assertEquals("true", filesets.get(0).getAttributeValue("removed"));

    final List<Element> files = filesets.get(0).getChildren("file");
    assertEquals(1, files.size());
    assertEquals("file:///test/struts.xml", files.get(0).getText());

    final Element propertiesKeys = state.getChild("propertiesKeys");
    assertNotNull(propertiesKeys);
    assertEquals("true", propertiesKeys.getAttributeValue("disabled"));
  }
}
