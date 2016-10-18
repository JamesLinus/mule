/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toMessage;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.function.Supplier;

class DefaultSourceCallback<T, A extends Attributes> implements SourceCallback<T, A> {

  static class Builder<T, A extends Attributes> {

    private Builder() {
    }

    private DefaultSourceCallback<T, A> product = new DefaultSourceCallback();

    public Builder<T, A> setListener(Processor listener) {
      product.listener = listener;
      return this;
    }

    public Builder<T, A> setConfigName(String configName) {
      product.configName = configName;
      return this;
    }

    public Builder<T, A> setFlowConstruct(FlowConstruct flowConstruct) {
      product.flowConstruct = flowConstruct;
      return this;
    }

    public Builder<T, A> setExceptionCallback(ExceptionCallback exceptionCallback) {
      product.exceptionCallback = exceptionCallback;
      return this;
    }

    public Builder<T, A> setProcessingManager(MessageProcessingManager processingManager) {
      product.messageProcessingManager = processingManager;
      return this;
    }

    public Builder<T, A> setProcessContextSupplier(Supplier<MessageProcessContext> processContextSupplier) {
      product.processContextSupplier = processContextSupplier;
      return this;
    }

    public Builder<T, A> setCompletionHandlerSupplier(Supplier<CompletionHandler<Event, MessagingException>> completionHandlerSupplier) {
      product.completionHandlerSupplier = completionHandlerSupplier;
      return this;
    }

    public SourceCallback<T, A> build() {
      checkArgument(product.listener, "listener");
      checkArgument(product.configName, "configName");
      checkArgument(product.flowConstruct, "flowConstruct");
      checkArgument(product.exceptionCallback, "exceptionCallback");
      checkArgument(product.messageProcessingManager, "messageProcessingManager");
      checkArgument(product.processContextSupplier, "processContextSupplier");
      checkArgument(product.completionHandlerSupplier, "completionHandlerSupplier");

      return product;
    }

    private void checkArgument(Object value, String name) {
      Preconditions.checkArgument(value != null, name + " was not set");
    }
  }

  static Builder builder() {
    return new Builder();
  }

  private Processor listener;
  private String configName;
  private FlowConstruct flowConstruct;
  private ExceptionCallback exceptionCallback;
  private MessageProcessingManager messageProcessingManager;
  private Supplier<MessageProcessContext> processContextSupplier;
  private Supplier<CompletionHandler<Event, MessagingException>> completionHandlerSupplier;

  private DefaultSourceCallback() {
  }

  @Override
  public void handle(Result<T, A> result) {
    Event event = Event.builder(create(flowConstruct, configName))
        .message((InternalMessage) toMessage(result))
        .exchangePattern(REQUEST_RESPONSE).flow(flowConstruct)
        .build();

    messageProcessingManager.processMessage(
        new ExtensionFlowProcessingTemplate(event, listener, completionHandlerSupplier.get()),
        processContextSupplier.get());
  }

  @Override
  public void onSourceException(Throwable exception) {
    exceptionCallback.onException(exception);
  }
}
