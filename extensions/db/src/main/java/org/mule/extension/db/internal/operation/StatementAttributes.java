/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;

/**
 * Parameters to configure the executed statements
 *
 * @since 4.0
 */
public class StatementAttributes {

  /**
   * Indicates how many rows to fetch from the database when rows are read from a resultSet. This property is required when
   * streaming is {@code true}; in that case a default value (10) is used.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED)
  private Integer fetchSize;

  /**
   * Sets the limit for the maximum number of rows that any ResultSet object generated by this message processor can contain for
   * the given number. If the limit is exceeded, the excess rows are silently dropped.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED)
  private Integer maxRows;

  public Integer getFetchSize() {
    return fetchSize;
  }

  public Integer getMaxRows() {
    return maxRows;
  }
}
