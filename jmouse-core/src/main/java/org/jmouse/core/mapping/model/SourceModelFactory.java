package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.DescriptorResolver;

import java.util.Map;

public final class SourceModelFactory {

    public SourceModel wrap(Object source) {
        if (source == null) {
            return new NullSourceModel();
        }

        if (source instanceof Map<?, ?> map) {
            return new MapSourceModel(map);
        }

        ObjectDescriptor<?> descriptor = DescriptorResolver.describe(source.getClass());

        return new DescriptorSourceModel(source, descriptor);
    }

    private static final class NullSourceModel implements SourceModel {

        @Override
        public Object source() {
            return null;
        }

        @Override
        public Class<?> sourceType() {
            return Void.class;
        }

        @Override
        public org.jmouse.core.mapping.values.ValueKind kind() {
            return org.jmouse.core.mapping.values.ValueKind.NULL;
        }

        @Override
        public ObjectDescriptor<?> descriptor() {
            return null;
        }

        @Override
        public boolean has(String name) {
            return false;
        }

        @Override
        public Object read(String name) {
            return null;
        }

    }
}
