package org.jmouse.core.metadata;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public interface MethodDescriptor extends ExecutableDescriptor<Method> {

    ClassDescriptor getReturnType();

    class Implementation extends ExecutableDescriptor.Implementation<Method> implements MethodDescriptor {

        protected final ClassDescriptor returnType;

        Implementation(
                String name, Method internal,
                Set<AnnotationDescriptor> annotations,
                Collection<ParameterDescriptor> parameters,
                Collection<ClassDescriptor> exceptionTypes,
                ClassDescriptor returnType
        ) {
            super(name, internal, annotations, parameters, exceptionTypes);

            this.returnType = returnType;
        }

        @Override
        public ClassDescriptor getReturnType() {
            return returnType;
        }

        @Override
        public String toString() {
            return "%s : %s".formatted(Reflections.getMethodName(internal), returnType);
        }

    }

    class Builder extends ExecutableDescriptor.Builder<Builder, Method, MethodDescriptor> {

        private ClassDescriptor returnType;

        public Builder(String name) {
            super(name);
        }

        public Builder returnType(ClassDescriptor returnType) {
            this.returnType = returnType;
            return self();
        }

        @Override
        public MethodDescriptor build() {
            return new Implementation(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    Collections.unmodifiableSet(parameters),
                    Collections.unmodifiableSet(exceptionTypes),
                    returnType
            );
        }

    }

}
