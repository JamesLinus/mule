/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.extension.http.api.request.validator.ResponseValidatorException;
import org.mule.extension.http.internal.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.extension.api.annotation.param.Ignore;

import org.hamcrest.core.Is;
import org.junit.Test;

public class ModuleWithGlobalElementTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "module/flows-using-module-global-elements.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    //TODO until MULE-10383 is fixed both Socket and Http extensions will be exposed, instead of just the Http one
    return new Class[] {SocketsExtension.class, HttpConnector.class};
  }

  /**
   * The test cannot run with isolation due to http ext doesn't have anymore the mule-module.properties. This test needs to have
   * the complete access to all the classes and resources therefore it just returns the class loader that loaded the test class.
   * (taken from AbstractTlsRestrictedProtocolsAndCiphersTestCase test)
   *
   * @return the {@link ClassLoader} that loaded the test.
   */
  @Override
  protected ClassLoader getExecutionClassLoader() {
    return this.getClass().getClassLoader();
  }

  @Test
  @Ignore
  //TODO WIP MULE-10252 fix the test and stop using an external server for http-basic-auth
  public void testHttpDoLogin() throws Exception {
    Event muleEvent = flowRunner("testHttpDoLogin").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("success with basic-authentication for user: userLP"));
  }

  @Test
  @Ignore
  //TODO WIP MULE-10252 fix the test and stop using an external server for http-basic-auth
  public void testHttpDontLogin() throws Exception {
    try {
      flowRunner("testHttpDontLogin").run();
      fail("Should not have reach here");
    } catch (MessagingException me) {
      assertThat(me.getCause(), instanceOf(ResponseValidatorException.class));
      assertThat(me.getCause().getMessage(), Is.is("Response code 401 mapped as failure"));
    }
  }

  @Test
  @Ignore
  //TODO WIP MULE-10252 fix the test and stop using an external server for http-basic-auth
  public void testHttpDoLoginGonnet() throws Exception {
    Event muleEvent = flowRunner("testHttpDoLoginGonnet").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("success with basic-authentication for user: userGonnet"));
  }
}
