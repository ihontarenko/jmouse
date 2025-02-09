package org.jmouse.core.descriptive;

import org.jmouse.common.support.invocable.FieldDescriptor;
import org.jmouse.core.reflection.Reflections;

import java.util.*;

import static org.jmouse.core.reflection.Reflections.getShortName;

public interface ClassDescriptor extends ElementDescriptor<Class<?>>, ClassTypeInspector {

    Collection<MethodDescriptor> getMethods();

    Collection<ConstructorDescriptor> getConstructors();

    Collection<FieldDescriptor> getFields();

    class Implementation extends ElementDescriptor.Implementation<Class<?>> implements ClassDescriptor {

        private final Collection<ConstructorDescriptor> constructors;
        private final Collection<FieldDescriptor>       fields;
        private final Collection<MethodDescriptor>      methods;

        public Implementation(
                String name, Class<?> internal,
                Set<AnnotationDescriptor> annotations, Class<?> classType,
                Collection<ConstructorDescriptor> constructors,
                Collection<FieldDescriptor> fields,
                Collection<MethodDescriptor> methods
        ) {
            super(name, internal, annotations);

            this.constructors = constructors;
            this.fields = fields;
            this.methods = methods;
        }

        @Override
        public Class<?> getClassType() {
            return internal;
        }

        @Override
        public Collection<MethodDescriptor> getMethods() {
            return methods;
        }

        @Override
        public Collection<ConstructorDescriptor> getConstructors() {
            return constructors;
        }

        @Override
        public Collection<FieldDescriptor> getFields() {
            return fields;
        }

        @Override
        public String toString() {
            return "%s".formatted(getShortName(internal));
        }
    }

    class Builder extends ElementDescriptor.Builder<Builder, Class<?>, ClassDescriptor> {

        private final Set<ConstructorDescriptor> constructors = new HashSet<>();
        private final Set<FieldDescriptor>       fields       = new HashSet<>();
        private final Set<MethodDescriptor>      methods      = new HashSet<>();

        protected Builder(String name) {
            super(name);
        }

        public Builder constructor(ConstructorDescriptor constructor) {
            constructors.add(constructor);
            return self();
        }

        public Builder field(FieldDescriptor field) {
            fields.add(field);
            return self();
        }

        public Builder method(MethodDescriptor method) {
            methods.add(method);
            return self();
        }

        @Override
        public ClassDescriptor build() {
            return new Implementation(name, internal,
                    Collections.unmodifiableSet(annotations),
                    internal,
                    Collections.unmodifiableSet(constructors),
                    Collections.unmodifiableSet(fields),
                    Collections.unmodifiableSet(methods)
            );
        }

    }

}
