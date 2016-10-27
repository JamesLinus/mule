package org.mule.runtime.module.artifact.serializer;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

public class ArtifactClassLoaderIdProvider implements ClassLoaderIdProvider
{

    @Override
    public String getClassLoaderId(ClassLoader classLoader)
    {

        String id = null;
        if (classLoader instanceof ArtifactClassLoader)
        {
            id = ((ArtifactClassLoader) classLoader).getArtifactId();
        }
        return id;
    }
}
