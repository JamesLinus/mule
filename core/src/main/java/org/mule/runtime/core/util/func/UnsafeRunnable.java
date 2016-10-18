package org.mule.runtime.core.util.func;

@FunctionalInterface
public interface UnsafeRunnable {

  void run() throws Exception;
}
