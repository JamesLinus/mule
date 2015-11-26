/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.studio.model.element;

import org.mule.module.extension.studio.model.IEditorElementVisitor;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "encoding")
public class EncodingEditor extends BaseFieldEditorElement
{

    @Override
    public void accept(IEditorElementVisitor visitor)
    {
        visitor.visit(this);
    }
}