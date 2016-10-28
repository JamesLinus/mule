/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.config.spring.dsl.model.extension.ModuleExtension;
import org.mule.runtime.config.spring.dsl.model.extension.loader.ModuleExtensionStore;
import org.mule.runtime.config.spring.dsl.model.extension.schema.ModuleSchemaGenerator;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ModuleDelegatingEntityResolver implements EntityResolver {

  private final ModuleExtensionStore moduleExtensionStore;
  private final EntityResolver entityResolver;

  public ModuleDelegatingEntityResolver(ModuleExtensionStore moduleExtensionStore) {
    this.entityResolver = new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader());
    this.moduleExtensionStore = moduleExtensionStore;
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    InputSource inputSource = entityResolver.resolveEntity(publicId, systemId);
    if (inputSource == null) {
      inputSource = generateModuleXsd(publicId, systemId);
    }
    return inputSource;
  }

  private InputSource generateModuleXsd(String publicId, String systemId) {
    InputSource inputSource = null;
    if (moduleExtensionStore == null) {
      //TODO WIP MULE-10252 until we unify the plugins, we need a way to discover the extensions to generate the XSD properly. Right now the only way is passing through the instantiated object with all of them there
      return inputSource;
    }
    ModuleExtension module = moduleExtensionStore.lookupByNamespace(systemId);
    if (module != null) {
      InputStream schema = new ModuleSchemaGenerator().getSchema(module);
      inputSource = new InputSource(schema);
      inputSource.setPublicId(publicId);
      inputSource.setSystemId(systemId);
    }
    return inputSource;
  }
}
