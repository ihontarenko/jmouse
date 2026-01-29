package org.jmouse.core.mapping.plan.bean;

import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.bind.descriptor.structured.DescriptorResolver;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plan.support.AbstractObjectPlan;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

public final class JavaBeanPlan<T> extends AbstractObjectPlan<T> {

    private final Class<T>            targetType;
    private final ObjectDescriptor<T> descriptor;

    public JavaBeanPlan(InferredType targetType) {
        super(targetType);
        this.targetType = targetType.getRawType();
        this.descriptor = DescriptorResolver.ofBeanType(this.targetType);
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        ObjectAccessor accessor   = toObjectAccessor(source, context);
        T              instance   = instantiate();
        Class<?>       sourceType = accessor.getClassType();
        Class<?>       targetType = getTargetType().getClassType();

        for (PropertyDescriptor<T> property : descriptor.getProperties().values()) {
            if (!property.isWritable()) {
                continue;
            }

            String         propertyName   = property.getName();
            InferredType   propertyType   = property.getType().getJavaType();
            MappingContext mappingContext = context.appendPath(propertyName);
            Object         value          = applyValue(accessor, mappingContext, sourceType, targetType, propertyName);

            if (value == IgnoredValue.INSTANCE || value == null) {
                continue;
            }

            Object adapted;

            try {
                adapted = adaptValue(value, propertyType, mappingContext);
            } catch (Exception exception) {
                throw toMappingException(
                        "bean_property_adapt_failed",
                        "Failed to adapt property '%s' to '%s'".formatted(propertyName, propertyType),
                        exception
                );
            }

            try {
                property.getAccessor().writeValue(instance, adapted);
            } catch (Exception exception) {
                throw toMappingException(
                        "bean_property_write_failed",
                        "Failed to write property '%s'".formatted(propertyName),
                        exception
                );
            }
        }

        return instance;
    }

    private T instantiate() {
        try {
            if (targetType.isInterface()) {
                throw new MappingException(
                        "bean_instantiation_failed",
                        "Failed to instantiate target bean because target-type is an interface: " + targetType.getName()
                );
            }
            return JavaBean.of(targetType).getFactory(TypedValue.of(targetType)).create();
        } catch (Exception exception) {
            throw new MappingException(
                    "bean_instantiation_failed",
                    "Failed to instantiate target bean: " + targetType.getName(),
                    exception
            );
        }
    }

}
