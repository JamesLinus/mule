/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.Optional;

public final class SourceCallbackParameters {

  private final Optional<ResolverSet> onSuccessParameters;
  private final Optional<ResolverSet> onErrorParameters;

  public SourceCallbackParameters(Optional<ResolverSet> onSuccessParameters, Optional<ResolverSet> onErrorParameters) {
    this.onSuccessParameters = onSuccessParameters;
    this.onErrorParameters = onErrorParameters;
  }

  public Optional<ResolverSet> getOnSuccessParameters() {
    return onSuccessParameters;
  }

  public Optional<ResolverSet> getOnErrorParameters() {
    return onErrorParameters;
  }
}
