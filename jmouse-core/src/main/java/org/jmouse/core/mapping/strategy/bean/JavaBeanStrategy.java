package org.jmouse.core.mapping.strategy.bean;

import org.jmouse.core.access.JavaBean;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.errors.ErrorCodes;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.strategy.support.AbstractObjectStrategy;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

/**
 * Object mapping strategy for mutable JavaBeans (property-based mapping). 🫘
 *
 * <p>{@code JavaBeanStrategy} materializes a target bean instance and then iterates over writable
 * properties described by {@link ObjectDescriptor}. For each writable property it:</p>
 * <ol>
 *   <li>computes the raw source value (explicit mapping or default accessor lookup)</li>
 *   <li>adapts the value to the target property type via {@link #adaptValue(Object, InferredType, MappingContext)}</li>
 *   <li>writes the adapted value using the property accessor</li>
 * </ol>
 *
 * <p>Property mapping respects ignore semantics: when a property resolves to {@link org.jmouse.core.mapping.strategy.support.AbstractStrategy.IgnoredValue#INSTANCE},
 * the property is skipped.</p>
 *
 * <p>Path tracking is maintained by appending the current property name to the {@link MappingContext},
 * enabling precise diagnostics and error reporting.</p>
 *
 * @param <T> target bean type
 */
public final class JavaBeanStrategy<T> extends AbstractObjectStrategy<T> {

    /**
     * Execute JavaBean mapping.
     *
     * <p>If {@code source} is {@code null}, returns {@code null}.</p>
     *
     * @param sourceValue source object
     * @param typedValue typed target descriptor (type metadata + optional instance holder)
     * @param context mapping context
     * @return mapped bean instance
     * @throws MappingException if instantiation, adaptation, or property write fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public T execute(Object sourceValue, TypedValue<T> typedValue, MappingContext context) {
        if (sourceValue == null) {
            return null;
        }

        InferredType        targetType  = typedValue.getType();
        Class<T>            targetClass = (Class<T>) targetType.getClassType();
        JavaBean<T>         javaBean    = JavaBean.of(targetType);
        ObjectDescriptor<T> descriptor  = DescriptorResolver.ofBeanType(targetClass);

        ObjectAccessor source     = toObjectAccessor(sourceValue, context);
        T              instance   = instantiate(typedValue, javaBean);
        ObjectAccessor target     = toObjectAccessor(instance, context);
        Class<?>       sourceType = source.getClassType();

        for (PropertyDescriptor<T> property : descriptor.getProperties().values()) {
            if (!property.isWritable()) {
                continue;
            }

            String         propertyName   = property.getName();
            InferredType   propertyType   = property.getType().getJavaType();
            MappingContext mappingContext = context.appendPath(propertyName);
            Object         value          = applyValue(source, mappingContext, sourceType, targetClass, propertyName);

            if (value == IgnoredValue.INSTANCE || value == null) {
                continue;
            }

            Object adapted;

            try {
                adapted = adaptValue(value, getTypedValue(target, propertyName, propertyType), mappingContext);
            } catch (Exception exception) {
                throw toMappingException(
                        ErrorCodes.BEAN_PROPERTY_ADAPT_FAILED,
                        "Failed to adapt property '%s' to '%s'".formatted(propertyName, propertyType),
                        exception
                );
            }

            try {
                property.getAccessor().writeValue(instance, adapted);
            } catch (Exception exception) {
                throw toMappingException(
                        ErrorCodes.BEAN_PROPERTY_WRITE_FAILED,
                        "Failed to write property '%s'".formatted(propertyName),
                        exception
                );
            }
        }

        return instance;
    }

    /**
     * Resolve or create the target bean instance.
     *
     * <p>Instantiation strategy:</p>
     * <ul>
     *   <li>interfaces are rejected with {@link ErrorCodes#BEAN_INSTANTIATION_FAILED}</li>
     *   <li>if {@link TypedValue} carries an instance (or supplier), it is used first</li>
     *   <li>otherwise a {@link JavaBean} factory is used to create a new instance</li>
     * </ul>
     *
     * @param typedValue typed target descriptor
     * @param javaBean bean metadata/factory access
     * @return target instance
     * @throws MappingException on instantiation failure
     */
    private T instantiate(TypedValue<T> typedValue, JavaBean<T> javaBean) {
        InferredType targetType  = typedValue.getType();
        Class<?>     targetClass = targetType.getClassType();

        try {
            if (targetClass.isInterface()) {
                throw new MappingException(
                        ErrorCodes.BEAN_INSTANTIATION_FAILED,
                        "Failed to instantiate target bean because target-type is an interface: " + targetType.getName()
                );
            }

            T instance = typedValue.getValue().get();

            if (instance == null) {
                instance = javaBean.getFactory(typedValue).create();
            }

            return instance;
        } catch (Exception exception) {
            throw new MappingException(
                    ErrorCodes.BEAN_INSTANTIATION_FAILED,
                    "Failed to instantiate target bean: " + targetType.getName(),
                    exception
            );
        }
    }

}
