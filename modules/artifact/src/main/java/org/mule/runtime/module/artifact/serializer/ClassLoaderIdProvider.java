package org.mule.runtime.module.artifact.serializer;

public interface ClassLoaderIdProvider {

  String getClassLoaderId(ClassLoader classLoader);

}
