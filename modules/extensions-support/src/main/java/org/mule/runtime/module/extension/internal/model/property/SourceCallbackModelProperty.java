/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import org.mule.runtime.api.meta.model.ModelProperty;

import java.lang.reflect.Method;
import java.util.Optional;

public final class SourceCallbackModelProperty implements ModelProperty {

  private final Optional<Method> onSuccessMethod;
  private final Optional<Method> onErrorMethod;

  public SourceCallbackModelProperty(Optional<Method> onSuccessMethod, Optional<Method> onErrorMethod) {
    this.onSuccessMethod = onSuccessMethod;
    this.onErrorMethod = onErrorMethod;
  }

  public Optional<Method> getOnSuccessMethod() {
    return onSuccessMethod;
  }

  public Optional<Method> getOnErrorMethod() {
    return onErrorMethod;
  }

  @Override
  public String getName() {
    return "sourceCallbackModelProperty";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isExternalizable() {
    return false;
  }
}
