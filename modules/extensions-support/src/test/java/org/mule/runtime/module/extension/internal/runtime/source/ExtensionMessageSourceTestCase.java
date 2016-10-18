/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowables;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.MuleTestUtils.spyInjector;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.setRequires;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceContext;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.model.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import javax.resource.spi.work.Work;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionMessageSourceTestCase extends AbstractMuleContextTestCase {

  private static final String CONFIG_NAME = "myConfig";
  private static final String ERROR_MESSAGE = "ERROR";
  private static final String SOURCE_NAME = "source";
  private final RetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(0, 2);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private SourceAdapterFactory sourceAdapterFactory;

  @Mock
  private SourceCallbackFactory sourceCallbackFactory;

  @Mock
  private Supplier<MessageProcessContext> processContextSupplier;

  @Mock
  private ThreadingProfile threadingProfile;

  @Mock
  private WorkManager workManager;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Processor messageProcessor;

  @Mock
  private Supplier<CompletionHandler<Event, MessagingException>> completionHandlerSupplier;

  @Mock
  private FlowConstruct flowConstruct;

  @Mock
  private Source source;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionManagerAdapter extensionManager;

  @Mock
  private MessageProcessingManager messageProcessingManager;

  @Mock
  private ExceptionCallback exceptionCallback;

  @Mock
  private ExceptionEnricherFactory enricherFactory;

  @Mock
  private InternalMessage muleMessage;

  @Mock
  private ConfigurationProvider configurationProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConfigurationModel configurationModel;

  @Mock
  private ConfigurationInstance configurationInstance;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ResolverSet callbackParameters;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Event event;

  @Mock
  private Result result;

  private SourceAdapter sourceAdapter;
  private SourceCallback sourceCallback;
  private ExtensionMessageSource messageSource;

  @Before
  public void before() throws Exception {
    spyInjector(muleContext);
    when(threadingProfile.createWorkManager(anyString(), eq(muleContext.getConfiguration().getShutdownTimeout())))
        .thenReturn(workManager);

    sourceCallback = DefaultSourceCallback.builder()
        .setConfigName(CONFIG_NAME)
        .setFlowConstruct(flowConstruct)
        .setListener(messageProcessor)
        .setProcessContextSupplier(processContextSupplier)
        .setCompletionHandlerSupplier(completionHandlerSupplier)
        .setExceptionCallback(exceptionCallback)
        .build();
    when(sourceCallbackFactory.createSourceCallback(completionHandlerSupplier)).thenReturn(sourceCallback);
    sourceAdapter = new SourceAdapter(extensionModel,
                                      sourceModel,
                                      source,
                                      Optional.of(configurationInstance),
                                      sourceCallbackFactory,
                                      callbackParameters,
                                      callbackParameters);

    when(sourceAdapterFactory.createAdapter(of(configurationInstance), sourceCallbackFactory)).thenReturn(sourceAdapter);
    doAnswer(invocationOnMock -> {
      sourceCallback.handle(result);
      return null;
    }).when(source).onStart(sourceCallback);

    mockExceptionEnricher(sourceModel, null);
    when(sourceModel.getName()).thenReturn(SOURCE_NAME);
    when(sourceModel.getModelProperty(MetadataResolverFactoryModelProperty.class)).thenReturn(empty());
    setRequires(sourceModel, true, true);
    mockExceptionEnricher(extensionModel, null);
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());

    initialiseIfNeeded(retryPolicyTemplate, muleContext);

    muleContext.getRegistry().registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);

    when(flowConstruct.getMuleContext()).thenReturn(muleContext);

    mockSubTypes(extensionModel);
    when(configurationModel.getSourceModel(SOURCE_NAME)).thenReturn(of(sourceModel));
    when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(of(configurationProvider));
    when(configurationProvider.get(any())).thenReturn(configurationInstance);
    when(configurationProvider.getConfigurationModel()).thenReturn(configurationModel);
    when(configurationProvider.getName()).thenReturn(CONFIG_NAME);

    messageSource = getNewExtensionMessageSourceInstance();
  }

  @Test
  public void handleMessage() throws Exception {
    doAnswer(invocation -> {
      ((Work) invocation.getArguments()[0]).run();
      return null;
    }).when(workManager).scheduleWork(any(Work.class));

    messageSource.initialise();
    messageSource.start();

    verify(sourceCallback).handle(result);
  }

  @Test
  public void handleExceptionAndRestart() throws Exception {
    messageSource.initialise();
    messageSource.start();

    messageSource.onException(new ConnectionException(ERROR_MESSAGE));
    verify((Stoppable) source).stop();
    verify(workManager, never()).dispose();
    verify((Disposable) source).dispose();
    verify((Initialisable) source, times(2)).initialise();
    verify((Startable) source, times(2)).start();
    handleMessage();
  }

  @Test
  public void initialise() throws Exception {
    messageSource.initialise();
    verify(source, never()).onStart(sourceCallback);
    verify(muleContext.getInjector()).inject(source);
    verify((Initialisable) source).initialise();
  }

  @Test
  public void sourceShouldIsInstantiatedOnce() throws MuleException {
    messageSource.doInitialise();
    messageSource.start();
    verify(sourceAdapterFactory, times(1)).createAdapter(of(configurationInstance), sourceCallbackFactory);
  }

  @Test
  public void initialiseFailsWithInitialisationException() throws Exception {
    Exception e = mock(InitialisationException.class);
    doThrow(e).when(((Initialisable) source)).initialise();
    expectedException.expect(is(sameInstance(e)));

    messageSource.initialise();
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE))).when(source).start();

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(instanceOf(MuleRuntimeException.class)));
    assertThat(throwable.getCause(), is(instanceOf(RetryPolicyExhaustedException.class)));
    verify(source, times(3)).start();
  }

  @Test
  public void failWithNonConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception {
    doThrow(new IOException(ERROR_MESSAGE)).when(source).start();

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(instanceOf(MuleRuntimeException.class)));
    assertThat(getThrowables(throwable), hasItemInArray(instanceOf(IOException.class)));
    verify(source, times(1)).start();
  }

  @Test
  public void failWithConnectionExceptionWhenStartingAndGetsReconnected() throws Exception {
    doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE)))
        .doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE))).doNothing().when(source).start();

    messageSource.initialise();
    messageSource.start();
    verify(source, times(3)).start();
    verify(source, times(2)).stop();
  }

  @Test
  public void failOnExceptionWithConnectionExceptionAndGetsReconnected() throws Exception {
    messageSource.initialise();
    messageSource.start();
    messageSource.onException(new ConnectionException(ERROR_MESSAGE));

    verify(source, times(2)).start();
    verify(source, times(1)).stop();
  }

  @Test
  public void failOnExceptionWithNonConnectionExceptionAndGetsExhausted() throws Exception {
    initialise();
    messageSource.start();
    messageSource.onException(new RuntimeException(ERROR_MESSAGE));

    verify(source, times(1)).start();
    verify(source, times(1)).stop();
  }

  @Test
  public void initialiseFailsWithRandomException() throws Exception {
    Exception e = new RuntimeException();
    doThrow(e).when(((Initialisable) source)).initialise();
    expectedException.expectCause(is(sameInstance(e)));

    messageSource.initialise();
  }

  @Test
  public void start() throws Exception {
    initialise();
    messageSource.start();

    verify(workManager).start();
    verify((Startable) source).start();
  }

  @Test
  public void failedToCreateWorkManager() throws Exception {
    Exception e = new RuntimeException();
    when(threadingProfile.createWorkManager(anyString(), eq(muleContext.getConfiguration().getShutdownTimeout()))).thenThrow(e);
    initialise();

    final Throwable throwable = catchThrowable(messageSource::start);
    assertThat(throwable, is(sameInstance(e)));
    verify((Startable) source, never()).start();
  }

  @Test
  public void stop() throws Exception {
    messageSource.initialise();
    messageSource.start();
    InOrder inOrder = inOrder(source, workManager);

    messageSource.stop();
    inOrder.verify((Stoppable) source).stop();
    inOrder.verify(workManager).dispose();
  }

  @Test
  public void enrichExceptionWithSourceExceptionEnricher() throws Exception {
    when(enricherFactory.createEnricher()).thenReturn(new HeisenbergConnectionExceptionEnricher());
    mockExceptionEnricher(sourceModel, enricherFactory);
    mockExceptionEnricher(sourceModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).start();
    Throwable t = catchThrowable(messageSource::start);

    assertThat(ExceptionUtils.containsType(t, ConnectionException.class), is(true));
    assertThat(t.getMessage(), containsString(ENRICHED_MESSAGE + ERROR_MESSAGE));
  }

  @Test
  public void enrichExceptionWithExtensionEnricher() throws Exception {
    final String enrichedErrorMessage = "Enriched: " + ERROR_MESSAGE;
    ExceptionEnricher exceptionEnricher = mock(ExceptionEnricher.class);
    when(exceptionEnricher.enrichException(any(Exception.class))).thenReturn(new Exception(enrichedErrorMessage));
    when(enricherFactory.createEnricher()).thenReturn(exceptionEnricher);
    mockExceptionEnricher(extensionModel, enricherFactory);
    ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
    doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).start();
    Throwable t = catchThrowable(messageSource::start);

    assertThat(t.getMessage(), containsString(enrichedErrorMessage));
  }

  @Test
  public void workManagerDisposedIfSourceFailsToStart() throws Exception {
    messageSource.initialise();
    messageSource.start();

    Exception e = new RuntimeException();
    doThrow(e).when((Stoppable) source).stop();
    expectedException.expect(new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object item) {
        Exception exception = (Exception) item;
        return exception.getCause() instanceof MuleException && exception.getCause().getCause() == e;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Exception was not wrapped as expected");
      }
    });

    messageSource.stop();
    verify(workManager).dispose();
  }

  @Test
  public void dispose() throws Exception {
    messageSource.initialise();
    messageSource.start();
    messageSource.stop();
    messageSource.dispose();

    verify((Disposable) source).dispose();
  }

  private ExtensionMessageSource getNewExtensionMessageSourceInstance() throws MuleException {

    ExtensionMessageSource messageSource =
        new ExtensionMessageSource(extensionModel, sourceModel, sourceAdapterFactory, configurationProvider,
                                   threadingProfile, retryPolicyTemplate, extensionManager);
    messageSource.setListener(messageProcessor);
    messageSource.setFlowConstruct(flowConstruct);
    muleContext.getInjector().inject(messageSource);
    return messageSource;
  }
}
