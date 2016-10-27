/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAlias;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.annotation.param.NotNull;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultResolvesetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;

import java.util.Set;

public class NullSafeValueResolverWrapper<T> implements ValueResolver<T> {

  private final ValueResolver<T> delegate;
  private final ValueResolver<T> fallback;

  public static <T> ValueResolver<T> of(MetadataType metadataType,
                                        ValueResolver<T> delegate,
                                        Set<ModelProperty> modelProperties,
                                        MuleContext muleContext) {
    checkArgument(delegate != null, "delegate cannot be null");
    checkArgument(metadataType != null, "metadataType cannot be null");
    checkArgument(metadataType.getMetadataFormat() == JAVA, "Only JAVA types are supported");

    ValueHolder<ValueResolver> value = new ValueHolder<>();
    metadataType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        Class<?> clazz = getType(objectType);
        ResolverSet resolverSet = new ResolverSet();
        getAnnotatedFields(clazz, Parameter.class).forEach(field -> {
          Optional optional = field.getAnnotation(Optional.class);
          if (optional == null) {
            throw new IllegalParameterModelDefinitionException(
                format("Class '%s' cannot be used with '@%s' parameter since it contains non optional fields",
                       clazz.getName(), NotNull.class.getSimpleName()));

          }

          resolverSet.add(getAlias(field),
                          new TypeSafeExpressionValueResolver(getDefaultValue(optional), field.getType(), muleContext));
          ObjectBuilder<T> objectBuilder =
              new DefaultResolvesetBasedObjectBuilder(clazz, getGroupModelProperty(modelProperties), resolverSet);

          value.set(new ObjectBuilderValueResolver(objectBuilder));
        });
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        Class collectionClass = getType(arrayType);
        value.set(CollectionValueResolver.of(collectionClass, emptyList()));
      }

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        Class mapClass = getType(dictionaryType);
        value.set(MapValueResolver.of(mapClass, emptyList(), emptyList()));
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        throw new IllegalParameterModelDefinitionException(
            format("Cannot use @%s on type '%s'", NotNull.class.getSimpleName(), getType(metadataType)));
      }
    });

    return new NullSafeValueResolverWrapper<>(delegate, value.get());
  }

  private static java.util.Optional<ParameterGroupModelProperty> getGroupModelProperty(Set<ModelProperty> modelProperties) {
    return modelProperties.stream()
        .filter(p -> p instanceof ParameterGroupModelProperty)
        .map(p -> (ParameterGroupModelProperty) p)
        .findFirst();
  }

  private NullSafeValueResolverWrapper(ValueResolver<T> delegate, ValueResolver<T> fallback) {
    this.delegate = delegate;
    this.fallback = fallback;
  }

  @Override
  public T resolve(Event event) throws MuleException {
    T value = delegate.resolve(event);

    if (value == null) {
      value = fallback.resolve(event);
    }

    return value;
  }

  @Override
  public boolean isDynamic() {
    return delegate.isDynamic();
  }
}
