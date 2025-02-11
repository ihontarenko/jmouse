package org.jmouse.core.metadata.object;

import org.jmouse.core.metadata.*;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Collections;
import java.util.Set;

public interface JavaBeanPropertyDescriptor<T> extends PropertyDescriptor<T> {

    MethodDescriptor getGetterMethod();

    MethodDescriptor getSetterMethod();

    class Implementation<T> extends PropertyDescriptor.Implementation<T> implements JavaBeanPropertyDescriptor<T> {

        private final MethodDescriptor getterMethod;
        private final MethodDescriptor setterMethod;

        Implementation(
                String name,
                T internal,
                Set<AnnotationDescriptor> annotations,
                Getter<T, Object> getter,
                Setter<T, Object> setter,
                MethodDescriptor getterMethod,
                MethodDescriptor setterMethod,
                JavaBeanDescriptor<T> owner
        ) {
            super(name, internal, annotations, getter, setter, owner);

            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
        }

        @Override
        public ClassDescriptor getType() {
            // todo: refactor
            if (isReadable()) {
                return getGetterMethod().getReturnType();
            } else if (isWritable()) {
                return getSetterMethod().getParameters().iterator().next().getType();
            }

            return null;
        }

        @Override
        public MethodDescriptor getGetterMethod() {
            return getterMethod;
        }

        @Override
        public MethodDescriptor getSetterMethod() {
            return setterMethod;
        }

        @Override
        public Class<?> getClassType() {
            return getType() != null ? getType().getClassType() : null;
        }

        @Override
        public String toString() {
            return "[JB]: " + super.toString();
        }

    }

    class Builder<T> extends PropertyDescriptor.Builder<Builder<T>, T, JavaBeanPropertyDescriptor<T>> {

        private MethodDescriptor getterMethod;
        private MethodDescriptor setterMethod;

        public Builder(String name) {
            super(name);
        }

        public Builder<T> getterMethod(MethodDescriptor getter) {
            this.getterMethod = getter;
            return self();
        }

        public Builder<T> setterMethod(MethodDescriptor setter) {
            this.setterMethod = setter;
            return self();
        }

        public Builder<T> owner(JavaBeanDescriptor<T> owner) {
            this.owner = owner;
            return self();
        }

        @Override
        public JavaBeanPropertyDescriptor<T> build() {
            return new Implementation<>(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    getter,
                    setter,
                    getterMethod,
                    setterMethod,
                    owner
            );
        }
    }
}
