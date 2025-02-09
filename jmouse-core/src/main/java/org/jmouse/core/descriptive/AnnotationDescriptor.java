package org.jmouse.core.descriptive;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public interface AnnotationDescriptor extends Descriptor<Annotation> {

    ClassDescriptor getAnnotationType();

    Map<String, Object> getAttributes();

    class Implementation extends Descriptor.Implementation<Annotation> implements AnnotationDescriptor {

        private final Map<String, Object> attributes = new HashMap<>();
        private final ClassDescriptor     annotationType;

        protected Implementation(String name, Annotation internal, Map<String, Object> attributes, ClassDescriptor annotationType) {
            super(name, internal);
            this.attributes.putAll(attributes);
            this.annotationType = annotationType;
        }

        @Override
        public ClassDescriptor getAnnotationType() {
            return annotationType;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

    }

    class Builder extends Descriptor.DescriptorBuilder<Builder, Annotation, AnnotationDescriptor> {

        private Map<String, Object> attributes = new HashMap<>();
        private ClassDescriptor     annotationType;

        protected Builder(String name) {
            super(name);
        }

        public Builder attribute(String name, Object value) {
            attributes.put(name, value);
            return self();
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return self();
        }

        public Builder annotationType(ClassDescriptor annotationType) {
            this.annotationType = annotationType;
            return self();
        }

        @Override
        public AnnotationDescriptor build() {
            return new Implementation(name, internal, attributes, annotationType);
        }

    }

}
