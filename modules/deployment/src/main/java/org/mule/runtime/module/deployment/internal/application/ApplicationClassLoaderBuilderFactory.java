/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.application.ApplicationClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;

/**
 * Factory to create instances of {@code ApplicationClassLoaderBuilder}.
 *
 * @since 4.0
 */
public class ApplicationClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory;
  private final ArtifactPluginRepository artifactPluginRepository;
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;

  /**
   * Creates an {@code ApplicationClassLoaderBuilderFactory} to create {@code ApplicationClassLoaderBuilder} instances.
   * 
   * @param applicationClassLoaderFactory factory for the class loader of the artifact resources and classes
   * @param artifactPluginRepository repository for artifact plugins provided by the runtime
   * @param artifactPluginClassLoaderFactory creates artifact plugin class loaders. Non null.
   */
  public ApplicationClassLoaderBuilderFactory(DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory,
                                              ArtifactPluginRepository artifactPluginRepository,
                                              ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory) {
    checkArgument(artifactPluginClassLoaderFactory != null, "artifactPluginClassLoaderFactory cannot be null");
    this.applicationClassLoaderFactory = applicationClassLoaderFactory;
    this.artifactPluginRepository = artifactPluginRepository;
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
  }

  /**
   * Creates a new {@code ApplicationClassLoaderBuilder} instance to create the application artifact class loader.
   *
   * @return a {@code ApplicationClassLoaderBuilder} instance.
   */
  public ApplicationClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new ApplicationClassLoaderBuilder(applicationClassLoaderFactory, artifactPluginRepository,
                                             artifactPluginClassLoaderFactory);
  }

}
