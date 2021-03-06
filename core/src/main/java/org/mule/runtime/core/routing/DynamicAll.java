/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RouterResultsHandler;

/**
 *
 * Routes a message through a set of routes that will be obtained dynamically (per message) using a {@link DynamicRouteResolver}.
 *
 * The message will be route to all the routes returned by {@link DynamicRouteResolver} and then all the results will be
 * aggregated.
 *
 */
public class DynamicAll implements Processor, MuleContextAware, Initialisable {

  private MulticastingRoutingStrategy routingStrategy;
  private DynamicRouteResolver dynamicRouteResolver;
  private MuleContext muleContext;
  private RouterResultsHandler resultAggregator = new DefaultRouterResultsHandler();

  @Override
  public void initialise() throws InitialisationException {
    routingStrategy = new MulticastingRoutingStrategy(muleContext, resultAggregator);
  }

  @Override
  public Event process(Event event) throws MuleException {
    return routingStrategy.route(event, dynamicRouteResolver.resolveRoutes(event));
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  /**
   * @param routeResolver custom route resolver to use
   */
  public void setDynamicRouteResolver(DynamicRouteResolver routeResolver) {
    this.dynamicRouteResolver = routeResolver;
  }

  /**
   * @param routerResultsHandler result aggregator to use after routing through the routes
   */
  public void setResultAggregator(RouterResultsHandler routerResultsHandler) {
    this.resultAggregator = routerResultsHandler;
  }
}
