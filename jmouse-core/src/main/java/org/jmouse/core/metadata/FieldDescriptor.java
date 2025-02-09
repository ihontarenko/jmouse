package org.jmouse.core.metadata;

import java.lang.reflect.Field;
import java.util.Set;

public interface FieldDescriptor extends ElementDescriptor<Field> {

    ClassDescriptor getType();

    class Implemetation extends ElementDescriptor.Implementation<Field> implements FieldDescriptor {

        private final ClassDescriptor type;

        public Implemetation(String name, Field internal, Set<AnnotationDescriptor> annotations, ClassDescriptor type) {
            super(name, internal, annotations);
            this.type = type;
        }

        @Override
        public ClassDescriptor getType() {
            return type;
        }

    }

}
