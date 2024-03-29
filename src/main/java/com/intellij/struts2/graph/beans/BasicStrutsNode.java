/*
 * Copyright 2008 The authors
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
package com.intellij.struts2.graph.beans;

import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Base class for nodes.
 *
 * @author Yann C&eacute;bron
 * @author Sergey Vasiliev
 */
public abstract class BasicStrutsNode<T extends DomElement> {

  private final T myIdentifyingElement;

  @NonNls
  private final @NotNull String myName;

  /**
   * CTOR.
   *
   * @param identifyingElement Underlying DOM-element.
   * @param name               Display name.
   */
  protected BasicStrutsNode(@NotNull final T identifyingElement, @Nullable @NonNls final String name) {
    myIdentifyingElement = identifyingElement;
    myName = name != null ? name : "";
  }

  @NonNls
  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public T getIdentifyingElement() {
    return myIdentifyingElement;
  }

  @NotNull
  public abstract Icon getIcon();

  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final BasicStrutsNode<?> pagesNode = (BasicStrutsNode<?>)o;
    return myIdentifyingElement.equals(pagesNode.myIdentifyingElement) &&
           myName.equals(pagesNode.myName);
  }

  public int hashCode() {
    int result;
    result = myIdentifyingElement.hashCode();
    result = 31 * result + myName.hashCode();
    return result;
  }

}