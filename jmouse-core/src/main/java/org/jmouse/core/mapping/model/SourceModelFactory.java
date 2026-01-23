package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.descriptor.structured.DescriptorResolver;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.mapping.errors.support.MappingFailures;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.values.ValueKind;
import org.jmouse.core.reflection.TypeInformation;

import java.util.Map;

public interface SourceModelFactory {
    SourceModel wrap(Object source);

    static SourceModelFactory defaultFactory(MappingContext ctx) {
        return source -> {
            if (source == null) {
                return new NullSourceModel();
            } else if (source instanceof Map<?, ?> map) {
                return new MapSourceModel(map);
            }

            return new DescriptorSourceModel(source, DescriptorResolver.describe(source.getClass()));
        };
    }

    final class NullSourceModel implements SourceModel {
        public Object source() { return null; }
        public Class<?> sourceType() { return Void.class; }
        public ValueKind kind() { return ValueKind.NULL; }
        public ObjectDescriptor<?> descriptor() { return null; }
        public boolean has(String name) { return false; }
        public Object read(String name) { return null; }
    }

    final class MapSourceModel implements SourceModel {

        private final Map<?, ?> map;

        MapSourceModel(Map<?, ?> map) {
            this.map = map;
        }

        public Object source() {
            return map;
        }

        public Class<?> sourceType() {
            return map.getClass();
        }

        public ValueKind kind() {
            return ValueKind.MAP;
        }

        public ObjectDescriptor<?> descriptor() {
            return null;
        }

        public boolean has(String name) {
            return map.containsKey(name);
        }

        public Object read(String name) {
            return map.get(name);
        }
    }

    final class DescriptorSourceModel implements SourceModel {

        private final Object              source;
        private final ObjectDescriptor<?> descriptor;

        DescriptorSourceModel(Object source, ObjectDescriptor<?> descriptor) {
            this.source = source;
            this.descriptor = descriptor;
        }

        public Object source() {
            return source;
        }

        public Class<?> sourceType() {
            return source.getClass();
        }

        public ValueKind kind() {
            return ValueKind.ofClassifier(TypeInformation.forClass(sourceType()));
        }

        public ObjectDescriptor<?> descriptor() {
            return descriptor;
        }

        public boolean has(String name) {
            return descriptor.hasProperty(name) && descriptor.getProperty(name).isReadable();
        }

        @SuppressWarnings("unchecked")
        public Object read(String name) {
            var propertyDescriptor = descriptor.getProperty(name);

            if (!descriptor.hasProperty(name)) {
                //                MappingFailures.fail(...)
            }

            if (!propertyDescriptor.isReadable()) {
//                MappingFailures.fail(...)
            }

            PropertyAccessor<Object> accessor = (PropertyAccessor<Object>) propertyDescriptor.getAccessor();

            return accessor.readValue(source);
        }
    }
}
