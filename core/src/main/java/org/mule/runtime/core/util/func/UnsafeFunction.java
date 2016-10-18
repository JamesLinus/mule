package org.mule.runtime.core.util.func;

@FunctionalInterface
public interface UnsafeFunction<T, R> {

  R apply(T value) throws Exception;
}
