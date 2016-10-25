/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.validation;

import static junit.framework.TestCase.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.config.RegisterServicesConfigurationBuilder;

import java.util.List;

abstract class AbstractValidateSchemaFunctionalTestCase extends FunctionalTestCase {

  protected static final String VALIDATE_FLOW = "validate";

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new RegisterServicesConfigurationBuilder());
  }

  protected void runAndExpectFailure(Object payload) throws Throwable {
    try {
      flowRunner(VALIDATE_FLOW).withPayload(payload).run();
      fail("was expecting a failure");
    } catch (MessagingException e) {
      throw e.getCause();
    }
  }
}
