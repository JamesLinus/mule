/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.serializer;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.serialization.SerializationException;
import org.mule.runtime.core.serialization.internal.AbstractSerializationProtocol;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Custom serialization protocol that uses {@link ArtifactClassLoaderObjectInputStream} and {@link ArtifactClassLoaderObjectOutputStream}
 * to write and read serialized objects to support deserialization of non exported classes.
 */
public class CustomJavaSerializationProtocol extends AbstractSerializationProtocol {

  private final ArtifactClassLoaderRepository artifactClassLoaderRepository;

  /**
   * Creates a new serialization protocol to serialize/deserialize classes provided by any class loader
   * defined in the provided class loader repository.
   *
   * @param artifactClassLoaderRepository contains the registered classloaders that can be used to load serialized classes. Non null.
   */
  public CustomJavaSerializationProtocol(ArtifactClassLoaderRepository artifactClassLoaderRepository) {
    checkArgument(artifactClassLoaderRepository != null, "artifactClassLoaderRepository cannot be null");
    this.artifactClassLoaderRepository = artifactClassLoaderRepository;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected byte[] doSerialize(Object object) throws Exception {
    validateForSerialization(object);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(512);

    try (ObjectOutputStream out = new ArtifactClassLoaderObjectOutputStream(outputStream)) {
      out.writeObject(object);
    } catch (IOException ex) {
      throw new SerializationException("Cannot serialize object", ex);
    }
    return outputStream.toByteArray();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected <T> T doDeserialize(InputStream inputStream, ClassLoader classLoader) throws Exception {
    checkArgument(inputStream != null, "Cannot deserialize a null stream");
    checkArgument(classLoader != null, "Cannot deserialize with a null classloader");

    try (ObjectInputStream in = new ArtifactClassLoaderObjectInputStream(artifactClassLoaderRepository, inputStream)) {
      Object obj = in.readObject();

      return (T) obj;
    } catch (Exception ex) {
      throw new SerializationException("Cannot deserialize object", ex);
    }
  }

  private void validateForSerialization(Object object) {
    if (object != null && !(object instanceof Serializable)) {
      throw new SerializationException(String.format("Was expecting a Serializable type. %s was found instead",
                                                     object.getClass().getName()));
    }
  }
}
