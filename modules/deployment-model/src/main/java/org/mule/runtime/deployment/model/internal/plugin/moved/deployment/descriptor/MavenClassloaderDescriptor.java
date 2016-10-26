/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.deployment.descriptor;

import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.plugin.moved.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.moved.dependency.Scope;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.ClassloaderModel;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.MalformedClassloaderModelException;
import org.mule.runtime.deployment.model.internal.plugin.moved.dependency.DefaultArtifactDependency;
import org.mule.runtime.deployment.model.internal.plugin.moved.deployment.DefaultClassloaderModel;
import org.mule.runtime.deployment.model.internal.plugin.moved.resource.URLPluginResourceLoader;
import org.mule.runtime.module.artifact.net.MulePluginUrlStreamHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Represents a TODO MULE-10785
 *
 * @since 4.0
 */
public class MavenClassloaderDescriptor implements ClassloaderDescriptor {

  public static final String MAVEN = "maven";
  private final static File POM_DEPENDENCY_FILE = new File("META-INF", "dependencies" + File.separator + "pom.xml");

  @Override
  public String getId() {
    return MAVEN;
  }

  @Override
  public ClassloaderModel load(URL location, Map<String, Object> attributes) throws MalformedClassloaderModelException {
    return new DefaultClassloaderModel(parseRuntimeClasses(location), parseExportedPackages(attributes, "exportedPackages"),
                                       parseExportedPackages(attributes, "exportedResources"),
                                       parseDependencies(new URLPluginResourceLoader()
                                           .loadResource(location, POM_DEPENDENCY_FILE.getPath())));
  }

  private Optional<URL> parseRuntimeClasses(URL location) throws MalformedClassloaderModelException {
    boolean isZip = location.getFile().endsWith(".zip");
    try {
      return Optional.of(isZip ? new URL(MulePluginUrlStreamHandler.PROTOCOL + ":" + location + "!/" + "classes" + "!/")
          : new URL(location, "classes"));
    } catch (MalformedURLException e) {
      throw new MalformedClassloaderModelException("Cannot assembly /classes URL", e);
    }
  }

  private HashSet<String> parseExportedPackages(Map<String, Object> attributes, String key)
      throws MalformedClassloaderModelException {
    try {
      List<String> elements = (List<String>) attributes.get(key);
      if (elements == null) {
        elements = new ArrayList<>();
      }
      return new HashSet<>(elements);
    } catch (ClassCastException e) {
      throw new MalformedClassloaderModelException(format("Cannot consume %s from the current descriptor as it does not match to a List of strings",
                                                          key));
    }
  }

  private Set<ArtifactDependency> parseDependencies(Optional<InputStream> pomInputStream)
      throws MalformedClassloaderModelException {
    Set<ArtifactDependency> dependencies = new HashSet<>();
    if (!pomInputStream.isPresent()) {
      return dependencies;
    }
    try {
      Model model = new MavenXpp3Reader().read(pomInputStream.get());
      for (Dependency dependency : model.getDependencies()) {
        ArtifactDependency artifactDependency =
            new DefaultArtifactDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                                          dependency.getType(), dependency.getClassifier(),
                                          dependency.getScope() != null ? Scope.valueOf(dependency.getScope().toUpperCase())
                                              : null);
        dependencies.add(artifactDependency);
      }
    } catch (IOException | XmlPullParserException e) {
      throw new MalformedClassloaderModelException("There was a problem while reading the pom file", e);
    }
    return dependencies;
  }
}
