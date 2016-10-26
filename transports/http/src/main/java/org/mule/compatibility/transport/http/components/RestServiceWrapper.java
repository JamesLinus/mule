/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import static org.mule.compatibility.transport.http.HttpConnector.HTTP_METHOD_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.FORM_URLENCODED_CONTENT_TYPE;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.config.i18n.CoreMessages.failedToInvokeRestService;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.component.AbstractComponent;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.functional.Either;
import org.mule.runtime.core.routing.filters.ExpressionFilter;
import org.mule.runtime.internal.utils.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service can used to proxy REST style services as local Mule Components. It can be configured with a service URL plus a
 * number of properties that allow you to configure the parameters and error conditions on the service.
 */
public class RestServiceWrapper extends AbstractComponent {

  public static final String DELETE = HttpConstants.METHOD_DELETE;
  public static final String GET = HttpConstants.METHOD_GET;
  public static final String CONTENT_TYPE_VALUE = FORM_URLENCODED_CONTENT_TYPE;
  public static final String HTTP_METHOD = "http.method";

  /**
   * logger used by this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private String serviceUrl;
  private Map requiredParams = new HashMap();
  private Map optionalParams = new HashMap();
  private String httpMethod = GET;
  private List payloadParameterNames;
  private Filter errorFilter;

  public String getServiceUrl() {
    return serviceUrl;
  }

  public void setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  public Map getRequiredParams() {
    return requiredParams;
  }

  /**
   * Required params that are pulled from the message. If these params don't exist the call will fail.
   *
   * @param requiredParams
   */
  public void setRequiredParams(Map requiredParams) {
    this.requiredParams = requiredParams;
  }

  /**
   * Optional params that are pulled from the message. If these params don't exist execution will continue.
   */
  public Map getOptionalParams() {
    return optionalParams;
  }

  public void setOptionalParams(Map optionalParams) {
    this.optionalParams = optionalParams;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public List getPayloadParameterNames() {
    return payloadParameterNames;
  }

  public void setPayloadParameterNames(List payloadParameterNames) {
    this.payloadParameterNames = payloadParameterNames;
  }

  public Filter getFilter() {
    return errorFilter;
  }

  public void setFilter(Filter errorFilter) {
    this.errorFilter = errorFilter;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    if (serviceUrl == null) {
      throw new InitialisationException(CoreMessages.objectIsNull("serviceUrl"), this);
    } else if (!muleContext.getExpressionLanguage().isExpression(serviceUrl)) {
      try {
        new URL(serviceUrl);
      } catch (MalformedURLException e) {
        throw new InitialisationException(e, this);
      }
    }

    if (errorFilter == null) {
      // We'll set a default filter that checks the return code
      errorFilter = new ExpressionFilter("#[message.inboundProperties['http.status']!=200]");
      ((ExpressionFilter) errorFilter).setMuleContext(muleContext);
      logger.info("Setting default error filter to ExpressionFilter('#[message.inboundProperties['http.status']!=200]')");
    }
  }

  @Override
  public Object doInvoke(Event event, Event.Builder eventBuilder) throws Exception {
    Object requestBody;

    InternalMessage message = event.getMessage();
    Object request = message.getPayload().getValue();
    String tempUrl = serviceUrl;
    if (muleContext.getExpressionLanguage().isExpression(serviceUrl)) {
      muleContext.getExpressionLanguage().validate(serviceUrl);
      tempUrl = muleContext.getExpressionLanguage().parse(serviceUrl, event, flowConstruct);
    }

    StringBuilder urlBuffer = new StringBuilder(tempUrl);

    if (GET.equalsIgnoreCase(this.httpMethod) || DELETE.equalsIgnoreCase(this.httpMethod)) {
      requestBody = null;

      setRESTParams(urlBuffer, event, request, requiredParams, false, null);
      setRESTParams(urlBuffer, event, request, optionalParams, true, null);
    }
    // if post
    else {
      if (MediaType.ANY.matches(message.getPayload().getDataType().getMediaType())) {
        message = InternalMessage.builder(message).mediaType(MediaType.parse(CONTENT_TYPE_VALUE)).build();
      }

      StringBuilder requestBodyBuffer = new StringBuilder();
      setRESTParams(urlBuffer, event, request, requiredParams, false, requestBodyBuffer);
      setRESTParams(urlBuffer, event, request, optionalParams, true, requestBodyBuffer);
      requestBody = requestBodyBuffer.toString();
    }

    tempUrl = urlBuffer.toString();
    logger.info("Invoking REST service: " + tempUrl);

    message = InternalMessage.builder(message).addOutboundProperty(HTTP_METHOD_PROPERTY, httpMethod).build();

    EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(tempUrl, muleContext);
    endpointBuilder.setExchangePattern(REQUEST_RESPONSE);
    OutboundEndpoint outboundEndpoint = endpointBuilder.buildOutboundEndpoint();

    Either<Error, InternalMessage> clientResponse = muleContext.getClient().send(outboundEndpoint.getEndpointURI().toString(),
                                                                                 InternalMessage.builder(message)
                                                                                     .payload(requestBody).build());

    if (clientResponse.isLeft()) {
      handleException(new RestServiceException(CoreMessages.failedToInvokeRestService(tempUrl)));
    }

    Event result = Event.builder(event.getContext()).message(clientResponse.getRight()).flow(flowConstruct).build();
    Builder builder = Event.builder(result);
    if (isErrorPayload(result, builder)) {
      handleException(new RestServiceException(failedToInvokeRestService(tempUrl)));
    }

    return builder.build().getMessage();
  }

  private String getSeparator(String url) {
    String sep;

    if (url.indexOf("?") > -1) {
      sep = "&";
    } else {
      sep = "?";
    }

    return sep;
  }

  private String updateSeparator(String sep) {
    if (sep.compareTo("?") == 0 || sep.compareTo("") == 0) {
      return ("&");
    }

    return sep;
  }

  // if requestBodyBuffer is null, it means that the request is a GET, otherwise it
  // is a POST and
  // requestBodyBuffer must contain the body of the http method at the end of this
  // function call
  private void setRESTParams(StringBuilder url, Event event, Object body, Map args, boolean optional,
                             StringBuilder requestBodyBuffer) {
    String sep;

    if (requestBodyBuffer == null) {
      sep = getSeparator(url.toString());
    } else if (requestBodyBuffer.length() > 0) {
      sep = "&";
    } else {
      sep = StringUtils.EMPTY;
    }

    for (Iterator iterator = args.entrySet().iterator(); iterator.hasNext();) {
      Map.Entry entry = (Map.Entry) iterator.next();
      String name = (String) entry.getKey();
      String exp = (String) entry.getValue();
      Object value = null;

      if (muleContext.getExpressionLanguage().isExpression(exp)) {
        muleContext.getExpressionLanguage().validate(exp);
        value = muleContext.getExpressionLanguage().evaluate(exp, event, flowConstruct);
      } else {
        value = exp;
      }

      if (value == null) {
        if (!optional) {
          throw new IllegalArgumentException(CoreMessages.propertyIsNotSetOnEvent(exp).toString());
        }
      } else if (requestBodyBuffer != null) // implies this is a POST
      {
        requestBodyBuffer.append(sep);
        requestBodyBuffer.append(name).append('=').append(value);
      } else {
        url.append(sep);
        url.append(name).append('=').append(value);
      }

      sep = updateSeparator(sep);
    }

    if (!optional && payloadParameterNames != null) {
      if (body instanceof Object[]) {
        Object[] requestArray = (Object[]) body;
        for (int i = 0; i < payloadParameterNames.size(); i++) {
          if (requestBodyBuffer != null) {
            requestBodyBuffer.append(sep).append(payloadParameterNames.get(i)).append('=').append(requestArray[i].toString());
          } else {
            url.append(sep).append(payloadParameterNames.get(i)).append('=').append(requestArray[i].toString());
          }

          sep = updateSeparator(sep);
        }
      } else {
        if (payloadParameterNames.get(0) != null) {
          if (requestBodyBuffer != null) {
            requestBodyBuffer.append(payloadParameterNames.get(0)).append('=').append(body.toString());
          } else {
            url.append(sep).append(payloadParameterNames.get(0)).append('=').append(body.toString());
          }
        }
      }
    }
  }

  protected boolean isErrorPayload(Event event, Event.Builder builder) {
    return errorFilter != null && errorFilter.accept(event, builder);
  }

  protected void handleException(RestServiceException e) throws Exception {
    throw e;
  }

}
