/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleExtension {

  private final String name;
  private final String namespace;
  private List<ParameterExtension> properties = new ArrayList<>();
  private Map<String, OperationExtension> operations = new HashMap<>();
  private ComponentModel componentModel;
  private List<ComponentModel> globalElements = new ArrayList<>();

  public ModuleExtension(String name, String namespace, ComponentModel componentModel) {
    this.name = name;
    this.namespace = namespace;
    this.componentModel = componentModel;
  }

  public String getName() {
    return name;
  }

  public String getNamespace() {
    return namespace;
  }

  public ComponentModel getComponentModel() {
    return componentModel;
  }

  public List<ParameterExtension> getProperties() {
    return properties;
  }

  public void setProperties(List<ParameterExtension> properties) {
    this.properties = properties;
  }

  public Map<String, OperationExtension> getOperations() {
    return operations;
  }

  public void setOperations(Map<String, OperationExtension> operations) {
    this.operations = operations;
  }

  public List<ComponentModel> getGlobalElements() {
    return globalElements;
  }

  public void setGlobalElements(List<ComponentModel> globalElements) {
    this.globalElements = globalElements;
  }

  public boolean hasConfig() {
    return (!properties.isEmpty()) || (!globalElements.isEmpty());
  }
}
