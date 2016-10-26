/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.SOURCE_CALLBACK_CONTEXT_PARAM;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveMethodComponentExecutor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

import java.lang.reflect.Method;
import java.util.Optional;

class ReflectiveSourceCallbackExecutor implements SourceCallbackExecutor {

  private final ExtensionModel extensionModel;
  private final Optional<ConfigurationInstance> configurationInstance;
  private final SourceModel sourceModel;
  private final ResolverSet parameters;
  private final MuleContext muleContext;
  private final ReflectiveMethodComponentExecutor<SourceModel> executor;

  public ReflectiveSourceCallbackExecutor(ExtensionModel extensionModel,
                                          Optional<ConfigurationInstance> configurationInstance,
                                          SourceModel sourceModel,
                                          Object source,
                                          Method method,
                                          ResolverSet parameters,
                                          MuleContext muleContext) {

    this.extensionModel = extensionModel;
    this.configurationInstance = configurationInstance;
    this.sourceModel = sourceModel;
    this.parameters = parameters;
    this.muleContext = muleContext;
    executor = new ReflectiveMethodComponentExecutor<>(sourceModel, method, source);
  }

  @Override
  public Object execute(Event event, SourceCallbackContext context) throws Exception {
    return executor.execute(createExecutionContext(event, context));
  }

  private ExecutionContext<SourceModel> createExecutionContext(Event event, SourceCallbackContext callbackContext) {
    final ResolverSetResult resolverSetResult;
    try {
      resolverSetResult = parameters.resolve(event);
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Found exception trying to resolve parameters for source callback"), e);
    }
    ExecutionContextAdapter<SourceModel> executionContext = new DefaultExecutionContext<>(extensionModel,
                                                                                          configurationInstance,
                                                                                          resolverSetResult,
                                                                                          sourceModel,
                                                                                          event,
                                                                                          muleContext);

    executionContext.setVariable(SOURCE_CALLBACK_CONTEXT_PARAM, callbackContext);
    return executionContext;
  }
}
