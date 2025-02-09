package org.jmouse.core.metadata;

import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Set;

public interface ParameterDescriptor extends ElementDescriptor<Parameter> {

    ClassDescriptor getType();

    class Implementation extends ElementDescriptor.Implementation<Parameter> implements ParameterDescriptor {

        private final ClassDescriptor type;

        Implementation(String name, Parameter internal, Set<AnnotationDescriptor> annotations, ClassDescriptor type) {
            super(name, internal, annotations);
            this.type = type;
        }

        @Override
        public ClassDescriptor getType() {
            return type;
        }

    }

    class Builder extends ElementDescriptor.Builder<Builder, Parameter, ParameterDescriptor> {

        private ClassDescriptor type;

        protected Builder(String name) {
            super(name);
        }

        Builder type(ClassDescriptor type) {
            this.type = type;
            return self();
        }

        @Override
        public ParameterDescriptor build() {
            return new Implementation(name, internal, Collections.unmodifiableSet(annotations), type);
        }

    }

}
