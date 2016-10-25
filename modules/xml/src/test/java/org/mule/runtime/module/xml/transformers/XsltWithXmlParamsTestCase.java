/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.module.xml.util.XMLTestUtils;
import org.mule.tck.config.RegisterServicesConfigurationBuilder;

import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

public class XsltWithXmlParamsTestCase extends FunctionalTestCase {

  private static final String EXPECTED =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><result><body><just>testing</just></body><fromParam>value element</fromParam></result>";

  @Override
  protected String getConfigFile() {
    return "xslt-with-xml-param-config.xml";
  }

  @Override
  protected final void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new RegisterServicesConfigurationBuilder());
  }

  @Test
  public void xmlSourceParam() throws Exception {
    Event event = flowRunner("xmlSourceParam").withPayload(XMLTestUtils.toSource("simple.xml"))
        .withVariable("xml", XMLTestUtils.toSource("test.xml")).run();

    assertExpected(event);
  }

  @Test
  public void xmlStringParam() throws Exception {
    Event event = flowRunner("xmlStringParam").withPayload(XMLTestUtils.toSource("simple.xml"))
        .withVariable("xml", XMLTestUtils.toSource("test.xml")).run();

    assertExpected(event);
  }

  private void assertExpected(Event event) throws Exception {
    assertThat(XMLUnit.compareXML(event.getMessage().getPayload().getValue().toString(), EXPECTED).similar(), is(true));
  }


}
