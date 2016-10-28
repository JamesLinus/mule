/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.metadata.api.model.MetadataType;

import com.google.common.base.Optional;

public class ParameterExtension {

  private String name;
  private MetadataType type;
  private Optional<String> defaultValue;

  public ParameterExtension(String name, MetadataType type, String defaultValue) {
    this.name = name;
    this.type = type;
    this.defaultValue = Optional.fromNullable(defaultValue);
  }

  public String getName() {
    return name;
  }

  public MetadataType getType() {
    return type;
  }

  public Optional<String> getDefaultValue() {
    return defaultValue;
  }
}
