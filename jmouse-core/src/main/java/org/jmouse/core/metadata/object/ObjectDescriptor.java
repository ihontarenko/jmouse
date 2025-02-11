package org.jmouse.core.metadata.object;

import org.jmouse.core.metadata.AnnotationDescriptor;
import org.jmouse.core.metadata.ClassDescriptor;
import org.jmouse.core.metadata.ClassTypeInspector;
import org.jmouse.core.metadata.ElementDescriptor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface ObjectDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    Collection<? extends PropertyDescriptor<T>> getProperties();

    PropertyDescriptor<T> getProperty(String name);

    boolean hasProperty(String name);

    abstract class Implementation<T> extends ElementDescriptor.Implementation<T> implements ObjectDescriptor<T> {

        protected final ClassDescriptor                              type;
        protected final Map<String, ? extends PropertyDescriptor<T>> properties;

        Implementation(
                String name,
                T internal,
                Set<AnnotationDescriptor> annotations,
                ClassDescriptor type,
                Map<String, ? extends PropertyDescriptor<T>> properties
        ) {
            super(name, internal, annotations);
            this.type = type;
            this.properties = properties;
        }

        @Override
        public Collection<? extends PropertyDescriptor<T>> getProperties() {
            return properties.values();
        }

        @Override
        public PropertyDescriptor<T> getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public boolean hasProperty(String name) {
            return properties.containsKey(name);
        }

        @Override
        public String toString() {
            return type.toString();
        }
    }

    abstract class Builder<T> extends ElementDescriptor.Builder<ObjectDescriptor.Builder<T>, T, ObjectDescriptor<T>> {

        protected Map<String, PropertyDescriptor<T>> properties = new LinkedHashMap<>();
        protected T                                  bean;
        protected ClassDescriptor                    descriptor;

        public Builder(String name) {
            super(name);
        }

        public ObjectDescriptor.Builder<T> bean(T bean) {
            this.bean = bean;
            return internal(bean).self();
        }

        public ObjectDescriptor.Builder<T> descriptor(ClassDescriptor descriptor) {
            this.descriptor = descriptor;
            return self();
        }

        public ObjectDescriptor.Builder<T> properties(Map<String, PropertyDescriptor<T>> properties) {
            this.properties = properties;
            return self();
        }

        public ObjectDescriptor.Builder<T> property(PropertyDescriptor<T> property) {
            this.properties.put(property.getName(), property);
            return self();
        }

    }

}
