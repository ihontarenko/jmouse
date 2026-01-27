package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.binding.PropertyMapping;
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
        return Verify.nonNull(context, "context").mappingRegistry().find(a, b);
    }

    protected PropertyMapping getPropertyMapping(String name, TypeMappingSpecification specification) {
        return specification == null ? null : specification.find(name);
    }

    protected PropertyMapping hasPropertyMapping(String name, TypeMappingSpecification specification) {
        return getPropertyMapping(name, specification);
    }

    protected Object applyValue(ObjectAccessor accessor, MappingContext context, Class<?> a, Class<?> b, String name) {
        TypeMappingSpecification specification = getMappingSpecifications(context, a, b);
        ValueSupplier            safe          = () -> safeGet(accessor, name);

        if (specification != null) {
            PropertyMapping propertyMapping = getPropertyMapping(name, specification);
            return applyValue(accessor, context, propertyMapping, safe);
        }

        return safe.get();
    }

}
