package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.function.Supplier;

@FunctionalInterface
interface SourceCallbackFactory {

  SourceCallback createSourceCallback(Supplier<CompletionHandler<Event, MessagingException>> completionHandlerSupplier);
}
