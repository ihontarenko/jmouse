package org.jmouse.core.mapping.plan.bean;

import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.bind.descriptor.structured.DescriptorResolver;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractMappingPlan;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

public final class JavaBeanPlan<T> extends AbstractMappingPlan<T> implements MappingPlan<T> {

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

        ObjectAccessor accessor = sourceAccessor(source, context);
        T              target   = instantiate();

        for (PropertyDescriptor<T> property : descriptor.getProperties().values()) {
            if (!property.isWritable()) {
                continue;
            }

            String       propertyName = property.getName();
            InferredType propertyType = property.getType().getJavaType();

            Object value = applyPropertyBinding(
                    accessor,
                    context,
                    propertyName,
                    context.mappingRegistry().find(source.getClass(), propertyType.getClassType()),
                    () -> safeGet(accessor, propertyName)
            );

            if (value == IgnoredValue.INSTANCE) {
                continue;
            }

            if (value == null) {
                continue;
            }

            Object adapted;

            try {
                adapted = adaptValue(value, propertyType, context);
            } catch (Exception exception) {
                throw toMappingException(
                        "bean_property_adapt_failed",
                        "Failed to adapt property '%s' to '%s'".formatted(propertyName, propertyType),
                        exception
                );
            }

            try {
                property.getAccessor().writeValue(target, adapted);
            } catch (Exception exception) {
                throw toMappingException(
                        "bean_property_write_failed",
                        "Failed to write property '%s'".formatted(propertyName),
                        exception
                );
            }
        }

        return target;
    }

    private T instantiate() {
        try {
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
