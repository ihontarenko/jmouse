package org.jmouse.core.metadata;

import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public interface ExecutableDescriptor<E extends Executable> extends ElementDescriptor<E> {

    Collection<ParameterDescriptor> getParameters();

    Collection<ClassDescriptor> getExceptionTypes();

    abstract class Implementation<E extends Executable>
            extends ElementDescriptor.Implementation<E> implements ExecutableDescriptor<E> {

        protected final Collection<ParameterDescriptor> parameters;
        protected final Collection<ClassDescriptor>     exceptionTypes;

        public Implementation(
                String name, E internal,
                Set<AnnotationDescriptor> annotations,
                Collection<ParameterDescriptor> parameters,
                Collection<ClassDescriptor> exceptionTypes
        ) {
            super(name, internal, annotations);
            this.parameters = parameters;
            this.exceptionTypes = exceptionTypes;
        }

        @Override
        public Collection<ParameterDescriptor> getParameters() {
            return parameters;
        }

        @Override
        public Collection<ClassDescriptor> getExceptionTypes() {
            return exceptionTypes;
        }

    }

    abstract class Builder<B extends Builder<B, E, D>, E extends Executable, D extends ExecutableDescriptor<E>>
            extends ElementDescriptor.Builder<B, E, D> {

        protected Set<ParameterDescriptor> parameters     = Collections.emptySet();
        protected Set<ClassDescriptor>     exceptionTypes = Collections.emptySet();

        protected Builder(String name) {
            super(name);
        }

        public B parameters(Set<ParameterDescriptor> parameters) {
            this.parameters = parameters;
            return self();
        }

        public B parameters(ParameterDescriptor... parameters) {
            return parameters(Set.of(parameters));
        }

        public B exceptionTypes(Set<ClassDescriptor> exceptionTypes) {
            this.exceptionTypes = exceptionTypes;
            return self();
        }

        public B exceptionTypes(ClassDescriptor... exceptionTypes) {
            return exceptionTypes(Set.of(exceptionTypes));
        }


    }

}
