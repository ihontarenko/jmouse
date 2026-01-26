package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.values.ValueKind;
import org.jmouse.core.reflection.TypeInformation;

import java.util.Objects;

final class DescriptorSourceModel implements SourceModel {

    private final Object              source;
    private final ObjectDescriptor<?> descriptor;

    DescriptorSourceModel(Object source, ObjectDescriptor<?> descriptor) {
        this.source = Objects.requireNonNull(source, "source");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    @Override
    public Object source() {
        return source;
    }

    @Override
    public Class<?> sourceType() {
        return source.getClass();
    }

    @Override
    public ValueKind kind() {
        return ValueKind.ofClassifier(TypeInformation.forClass(sourceType()));
    }

    @Override
    public ObjectDescriptor<?> descriptor() {
        return descriptor;
    }

    @Override
    public boolean has(String name) {
        return descriptor.hasProperty(name) && descriptor.getProperty(name).isReadable();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object read(String name) {
        if (!descriptor.hasProperty(name)) {
            throw new MappingException("source_property_missing", "Source property not found: '" + name + "'");
        }

        var property = descriptor.getProperty(name);
        if (!property.isReadable()) {
            throw new MappingException("source_property_not_readable", "Source property not readable: '" + name + "'");
        }

        PropertyAccessor<Object> accessor = (PropertyAccessor<Object>) property.getAccessor();
        return accessor.readValue(source);
    }
}
