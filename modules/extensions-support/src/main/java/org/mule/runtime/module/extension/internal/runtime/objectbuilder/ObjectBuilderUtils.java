/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.instanciateClass;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.core.api.MuleRuntimeException;

class ObjectBuilderUtils {

  public static <T> T createInstance(Class<T> prototypeClass) {
    try {
      return withContextClassLoader(prototypeClass.getClassLoader(), () -> instanciateClass(prototypeClass));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + prototypeClass), e);
    }
  }

  private ObjectBuilderUtils() {
  }
}
