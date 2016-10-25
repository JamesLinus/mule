/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener.builder;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Component that specifies how to create a proper HTTP error response
 *
 * @since 4.0
 */
@Alias("error-response-builder")
@XmlHints(allowTopLevelDefinition = true)
public class HttpListenerErrorResponseBuilder extends HttpListenerResponseBuilder {

  /**
   * The body of the response message
   */
  @Parameter
  @Optional
  @XmlHints(allowReferences = false)
  private Object body;

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getBody() {
    return body;
  }
}
