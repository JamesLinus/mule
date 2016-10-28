/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.model.ModuleExtensionLoader;

import java.util.ArrayList;
import java.util.List;

public class OperationExtension {

  private String name;
  private List<ParameterExtension> parameters = new ArrayList<>();
  private ComponentModel componentModel;
  private MetadataType outputType;

  public OperationExtension(String name, ComponentModel componentModel) {
    this.name = name;
    this.componentModel = componentModel;
  }

  public String getName() {
    return name;
  }

  public List<ParameterExtension> getParameters() {
    return parameters;
  }

  public void setParameters(List<ParameterExtension> parameters) {
    this.parameters = parameters;
  }

  public ComponentModel getComponentModel() {
    return componentModel;
  }

  public void setOutputType(MetadataType outputType) {
    this.outputType = outputType;
  }

  public boolean returnsVoid() {
    ReturnsVoidTypeVisitor returnsVoidTypeVisitor = new ReturnsVoidTypeVisitor();
    outputType.accept(returnsVoidTypeVisitor);
    return returnsVoidTypeVisitor.returnsVoid;
  }


  public List<ComponentModel> getMessageProcessorsComponentModels() {
    return this.getComponentModel().getInnerComponents()
        .stream()
        .filter(childComponent -> childComponent.getIdentifier().equals(ModuleExtensionLoader.OPERATION_BODY_IDENTIFIER))
        .findAny().get().getInnerComponents();
  }

  /**
   * visits all the possible types of a given parameter to realize if it's a "void return type", in which case the
   * expanded chain will not modify the structure of the event
   */
  private class ReturnsVoidTypeVisitor extends MetadataTypeVisitor {

    private boolean returnsVoid = false;

    @Override
    public void visitVoid(VoidType voidType) {
      returnsVoid = true;
    }
  }
}
