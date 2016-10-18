/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.core.execution.NullCompletionHandler;
import org.mule.runtime.core.util.func.UnsafeFunction;
import org.mule.runtime.core.util.func.UnsafeRunnable;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.model.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveMethodComponentExecutor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An adapter for {@link Source} which acts as a bridge with {@link ExtensionMessageSource}.
 * It also propagates lifecycle and performs injection of both, dependencies and parameters
 *
 * @since 4.0
 */
public final class SourceAdapter implements Startable, Stoppable, FlowConstructAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceAdapter.class);

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;
  private final Source source;
  private final Optional<ConfigurationInstance> configurationInstance;
  private final Optional<FieldSetter<Object, Object>> configurationSetter;
  private final Optional<FieldSetter<Object, Object>> connectionSetter;
  private final SourceCallbackFactory sourceCallbackFactory;
  private final ResolverSet successCallbackParameters;
  private final ResolverSet errorCallbackParameters;

  private ConnectionHandler<Object> connectionHandler;
  private FlowConstruct flowConstruct;

  @Inject
  private ConnectionManager connectionManager;

  @Inject
  private MuleContext muleContext;

  public SourceAdapter(ExtensionModel extensionModel, SourceModel sourceModel,
                       Source source,
                       Optional<ConfigurationInstance> configurationInstance,
                       SourceCallbackFactory sourceCallbackFactory,
                       ResolverSet successCallbackParameters,
                       ResolverSet errorCallbackParameters) {
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.source = source;
    this.configurationInstance = configurationInstance;
    this.sourceCallbackFactory = sourceCallbackFactory;
    this.successCallbackParameters = successCallbackParameters;
    this.errorCallbackParameters = errorCallbackParameters;

    configurationSetter = fetchField(UseConfig.class);
    connectionSetter = fetchField(Connection.class);
  }

  private SourceCallback createSourceCallback() {
    return sourceCallbackFactory.createSourceCallback(this::createCompletionHandler);
  }

  private CompletionHandler<Event, MessagingException> createCompletionHandler() {
    return sourceModel.getModelProperty(SourceCallbackModelProperty.class)
        .map(this::doCreateCompletionHandler)
        .orElse(new NullCompletionHandler<>());
  }

  private CompletionHandler<Event, MessagingException> doCreateCompletionHandler(SourceCallbackModelProperty modelProperty) {
    final UnsafeFunction<Event, Object> onSuccessFunction;
    final UnsafeFunction<Event, Object> onErrorFunction;

    if (modelProperty.getOnSuccessMethod().isPresent()) {
      Method method = modelProperty.getOnSuccessMethod().get();
      ReflectiveMethodComponentExecutor<SourceModel> executor =
          new ReflectiveMethodComponentExecutor<>(sourceModel, method, source);
      onSuccessFunction = callbackEvent -> executor.execute(createExecutionContext(callbackEvent, successCallbackParameters));
    } else {
      onSuccessFunction = callbackEvent -> null;
    }

    if (modelProperty.getOnErrorMethod().isPresent()) {
      Method method = modelProperty.getOnErrorMethod().get();
      ReflectiveMethodComponentExecutor<SourceModel> executor =
          new ReflectiveMethodComponentExecutor<>(sourceModel, method, source);
      onErrorFunction = callbackEvent -> executor.execute(createExecutionContext(callbackEvent, errorCallbackParameters));
    } else {
      onErrorFunction = callbackEvent -> null;
    }

    return new SourceCompletionHandler(onSuccessFunction, onErrorFunction);
  }

  private class SourceCompletionHandler implements CompletionHandler<Event, MessagingException> {

    private final UnsafeFunction<Event, Object> onSuccessFunction;
    private final UnsafeFunction<Event, Object> onErrorFunction;

    private SourceCompletionHandler(
        UnsafeFunction<Event, Object> onSuccessFunction,
        UnsafeFunction<Event, Object> onErrorFunction) {
      this.onSuccessFunction = onSuccessFunction;
      this.onErrorFunction = onErrorFunction;
    }

    @Override
    public void onCompletion(Event result, ExceptionCallback exceptionCallback) {
      safely(() -> onSuccessFunction.apply(result), exceptionCallback);
    }

    @Override
    public void onFailure(MessagingException exception) {
      safely(() -> onErrorFunction.apply(exception.getEvent()), callbackException -> {
        if (LOGGER.isDebugEnabled()) {
          LOGGER
              .debug(format("Found exception trying to handle error from source '%s'", sourceModel.getName()), callbackException);
        }
      });
    }

    private void safely(UnsafeRunnable task, ExceptionCallback exceptionCallback) {
      try {
        task.run();
      } catch (Throwable e) {
        exceptionCallback.onException(e);
      }
    }
  }

  private ExecutionContext<SourceModel> createExecutionContext(Event event, ResolverSet parameters) {
    final ResolverSetResult resolverSetResult;
    try {
      resolverSetResult = parameters.resolve(event);
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Found exception trying to resolve parameters for source callback"), e);
    }
    return new DefaultExecutionContext<>(extensionModel,
                                         configurationInstance,
                                         resolverSetResult,
                                         sourceModel,
                                         event,
                                         muleContext);
  }

  @Override
  public void start() throws MuleException {
    if (source instanceof FlowConstructAware) {
      ((FlowConstructAware) source).setFlowConstruct(flowConstruct);
    }

    try {
      setConfiguration(configurationInstance);
      setConnection();
      source.onStart(createSourceCallback());
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  @Override
  public void stop() throws MuleException {
    try {
      source.onStop();
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    } finally {
      releaseConnection();
    }
  }

  private void setConfiguration(Optional<ConfigurationInstance> configuration) {
    if (configurationSetter.isPresent() && configuration.isPresent()) {
      configurationSetter.get().set(source, configuration.get().getValue());
    }
  }

  private void setConnection() {
    if (connectionSetter.isPresent() && configurationInstance.isPresent()) {
      try {
        connectionHandler = connectionManager.getConnection(configurationInstance.get().getValue());
        connectionSetter.get().set(source, connectionHandler.getConnection());
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage(format(
            "Could not obtain connection for message source '%s' on flow '%s'",
            getName(), flowConstruct.getName())), e);
      }
    }
  }

  private void releaseConnection() {
    if (connectionHandler != null) {
      try {
        connectionHandler.release();
      } finally {
        connectionHandler = null;
      }
    }
  }

  private <T> Optional<FieldSetter<Object, T>> fetchField(Class<? extends Annotation> annotation) {
    Set<Field> fields = getAllFields(source.getClass(), withAnnotation(annotation));
    if (CollectionUtils.isEmpty(fields)) {
      return Optional.empty();
    }

    if (fields.size() > 1) {
      // TODO: MULE-9220 Move this to a syntax validator
      throw new IllegalModelDefinitionException(
          format("Message Source defined on class '%s' has more than one field annotated with '@%s'. "
                     + "Only one field in the class can bare such annotation", source.getClass().getName(),
                 annotation.getClass().getSimpleName()));
    }

    return Optional.of(new FieldSetter<>(fields.iterator().next()));
  }

  public String getName() {
    return getSourceName(source.getClass());
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
