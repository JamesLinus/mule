/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.studio.model.element;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlRootElement
@XmlSeeAlso({StringEditor.class, FileEditor.class, PasswordEditor.class, PathEditor.class, UrlEditor.class})
public abstract class BaseStringEditor extends BaseFieldEditorElement
{

    private String defaultValue;

    @XmlAttribute
    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!super.equals(obj))
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        BaseStringEditor other = (BaseStringEditor) obj;
        if (defaultValue == null)
        {
            if (other.defaultValue != null)
            {
                return false;
            }
        }
        else if (!defaultValue.equals(other.defaultValue))
        {
            return false;
        }
        return true;
    }

}