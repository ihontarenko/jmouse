package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.binding.TypeMappingSpecification;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

abstract public class AbstractObjectPlan<T> extends AbstractPlan<T> {

    protected AbstractObjectPlan(InferredType targetType) {
        super(targetType);
    }

    protected Class<?> getTargetType(PropertyDescriptor<?> propertyDescriptor) {
        return propertyDescriptor.getType().getClassType();
    }

    protected boolean hasMappingSpecifications(MappingContext context, Class<?> a, Class<?> b) {
        return getMappingSpecifications(context, a, b) != null;
    }

    protected TypeMappingSpecification getMappingSpecifications(MappingContext context, Class<?> a, Class<?> b) {
        return context.mappingRegistry().find(a, b);
    }

}
