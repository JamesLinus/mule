/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.source;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.config.ThreadingProfile.DEFAULT_THREADING_PROFILE;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty.CallbackPhase.ON_ERROR;
import static org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty.CallbackPhase.ON_SUCCESS;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.config.ImmutableThreadingProfile;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty.CallbackPhase;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapter;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapterFactory;
import org.mule.runtime.module.extension.internal.runtime.source.SourceConfigurer;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;

import com.google.common.base.Joiner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * An {@link AbstractExtensionObjectFactory} that produces instances of {@link ExtensionMessageSource}
 *
 * @since 4.0
 */
public class ExtensionSourceObjectFactory extends AbstractExtensionObjectFactory<ExtensionMessageSource> {

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;
  private final MuleContext muleContext;

  private ConfigurationProvider configurationProvider;
  private RetryPolicyTemplate retryPolicyTemplate;

  @Inject
  private ConnectionManagerAdapter connectionManager;

  public ExtensionSourceObjectFactory(ExtensionModel extensionModel, SourceModel sourceModel,
                                      MuleContext muleContext) {
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.muleContext = muleContext;
  }

  @Override
  public ExtensionMessageSource getObject() throws ConfigurationException {
    checkParameterGroupExclusivenessForModel(sourceModel, getParameters().keySet());
    ResolverSet nonCallbackParameters = getNonCallbackParameters();

    if (nonCallbackParameters.isDynamic()) {
      throw dynamicParameterException(nonCallbackParameters, sourceModel);
    }

    ResolverSet responseCallbackParameters = getCallbackParameters(ON_SUCCESS);
    ResolverSet errorCallbackParameters = getCallbackParameters(ON_ERROR);

    ExtensionMessageSource messageSource =
        new ExtensionMessageSource(extensionModel,
                                   sourceModel,
                                   getSourceFactory(nonCallbackParameters, responseCallbackParameters, errorCallbackParameters),
                                   configurationProvider,
                                   getThreadingProfile(),
                                   getRetryPolicyTemplate(),
                                   (ExtensionManagerAdapter) muleContext.getExtensionManager());
    try {
      muleContext.getInjector().inject(messageSource);
    } catch (MuleException e) {
      throw new ConfigurationException(createStaticMessage("Could not inject dependencies into source"), e);
    }

    return messageSource;
  }

  private ResolverSet getNonCallbackParameters() {
    return getSourceParameters(empty());
  }

  private ResolverSet getCallbackParameters(CallbackPhase callbackPhase) {
    return getSourceParameters(of(callbackPhase));
  }

  private ResolverSet getSourceParameters(Optional<CallbackPhase> callbackPhase) {
    ResolverSet resolverSet = new ResolverSet();
    Map<String, Object> parameters = getParameters();
    sourceModel.getParameterModels().stream()
        .filter(p -> {
          final Optional<CallbackParameterModelProperty> property = p.getModelProperty(CallbackParameterModelProperty.class);
          if (property.isPresent() != callbackPhase.isPresent()) {
            return false;
          }

          if (property.isPresent() && callbackPhase.isPresent()) {
            return property.get().getCallbackPhase() == callbackPhase.get();
          }

          return false;
        })
        .forEach(p -> {
          if (parameters.containsKey(p.getName())) {
            resolverSet.add(p.getName(), toValueResolver(parameters.get(p.getName())));
          }
        });

    return resolverSet;
  }

  private ThreadingProfile getThreadingProfile() {
    ThreadingProfile tp = new ImmutableThreadingProfile(DEFAULT_THREADING_PROFILE);
    tp.setMuleContext(muleContext);

    return tp;
  }

  private SourceAdapterFactory getSourceFactory(ResolverSet nonCallbackParameters,
                                                ResolverSet successCallbackParameters,
                                                ResolverSet errorCallbackParameters) {
    return (configurationInstance, sourceCallbackFactory) -> {
      Source source = MuleExtensionUtils.getSourceFactory(sourceModel).createSource();
      try {
        source = new SourceConfigurer(sourceModel, nonCallbackParameters, muleContext).configure(source);
        return new SourceAdapter(extensionModel,
                                 sourceModel,
                                 source,
                                 configurationInstance,
                                 sourceCallbackFactory,
                                 successCallbackParameters,
                                 errorCallbackParameters);
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage(format("Could not create generator for source '%s'",
                                                                  sourceModel.getName())),
                                       e);
      }
    };
  }

  private RetryPolicyTemplate getRetryPolicyTemplate() throws ConfigurationException {
    return retryPolicyTemplate != null ? retryPolicyTemplate : connectionManager.getDefaultRetryPolicyTemplate();
  }

  public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
  }

  private ConfigurationException dynamicParameterException(ResolverSet resolverSet, SourceModel model) {
    List<String> dynamicParams = resolverSet.getResolvers().entrySet().stream().filter(entry -> entry.getValue().isDynamic())
        .map(entry -> entry.getKey()).collect(toList());

    return new ConfigurationException(createStaticMessage(format("The '%s' message source is using expressions, which are not allowed on message sources. "
        + "Offending parameters are: [%s]", model.getName(), Joiner.on(',').join(dynamicParams))));
  }

  public void setConfigurationProvider(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
  }
}
