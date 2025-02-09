package org.jmouse.core.metadata;

import java.util.Collections;
import java.util.Set;

public interface PropertyDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    ClassDescriptor getType();

    ClassDescriptor getOwner();

    MethodDescriptor getGetter();

    MethodDescriptor getSetter();

    default boolean isWritable() {
        return getSetter() != null;
    }

    default boolean isReadable() {
        return getGetter() != null;
    }

    class Implementation<T> extends ElementDescriptor.Implementation<T> implements PropertyDescriptor<T> {

        private final MethodDescriptor  getter;
        private final MethodDescriptor  setter;
        private final BeanDescriptor<T> owner;

        Implementation(
                String name, T internal,
                Set<AnnotationDescriptor> annotations,
                MethodDescriptor getter,
                MethodDescriptor setter,
                BeanDescriptor<T> owner
        ) {
            super(name, internal, annotations);
            this.getter = getter;
            this.setter = setter;
            this.owner = owner;
        }

        @Override
        public ClassDescriptor getType() {
            ClassDescriptor type = null;

            if (isReadable()) {
                type = getGetter().getReturnType();
            } else if (isWritable()) {
                type = getSetter().getParameters().iterator().next().getType();
            }

            return type;
        }

        @Override
        public ClassDescriptor getOwner() {
            return null;
        }

        @Override
        public MethodDescriptor getGetter() {
            return getter;
        }

        @Override
        public MethodDescriptor getSetter() {
            return setter;
        }

        /**
         * Returns the class type being inspected.
         *
         * @return the {@link Class} object representing the inspected type
         */
        @Override
        public Class<?> getClassType() {
            return getType().getClassType();
        }

    }

    class Builder<T> extends ElementDescriptor.Builder<Builder<T>, T, PropertyDescriptor<T>> {

        private MethodDescriptor  getter;
        private MethodDescriptor  setter;
        private BeanDescriptor<T> owner;

        /**
         * Constructs a new {@code ElementDescriptor.Builder} with the given name.
         *
         * @param name the name of the element being built
         */
        public Builder(String name) {
            super(name);
        }

        public Builder<T> getter(MethodDescriptor getter) {
            this.getter = getter;
            return self();
        }

        public Builder<T> setter(MethodDescriptor setter) {
            this.setter = setter;
            return self();
        }

        public Builder<T> owner(BeanDescriptor<T> owner) {
            this.owner = owner;
            return self();
        }

        /**
         * Constructs the final descriptor instance.
         *
         * @return a new instance of {@code D}
         */
        @Override
        public PropertyDescriptor<T> build() {
            return new Implementation<>(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    getter,
                    setter,
                    owner
            );
        }

    }

}
