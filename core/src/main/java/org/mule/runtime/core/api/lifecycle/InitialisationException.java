/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>InitialisationException</code> is thrown by the initialise method defined in the
 * <code>org.mule.runtime.core.api.lifecycle.Initialisable</code> interface. IinitialisationExceptions are fatal and will cause
 * the current Mule instance to shutdown.
 */
public class InitialisationException extends LifecycleException {

  /** Serial version */
  private static final long serialVersionUID = -8402348927606781931L;

  /**
   * @param message the exception message
   * @param component the object that failed during a lifecycle method call
   */
  public InitialisationException(I18nMessage message, Initialisable component) {
    super(message, component);
  }

  /**
   * @param message the exception message
   * @param cause the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public InitialisationException(I18nMessage message, Throwable cause, Initialisable component) {
    super(message, cause, component);
  }

  /**
   * @param cause the exception that cause this exception to be thrown
   * @param component the object that failed during a lifecycle method call
   */
  public InitialisationException(Throwable cause, Initialisable component) {
    super(cause, component);
  }
}
