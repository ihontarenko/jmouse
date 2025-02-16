package org.jmouse.core.bind.descriptor;

public interface Descriptor<T> {

    String getName();

    T unwrap();

    // ???
    Mutable<?, T, ?> toMutable();

    interface Mutable<M extends Mutable<M, T, D>, T, D extends Descriptor<T>> {

        M name(String name);

        M name();

        M introspect();

        D toImmutable();

        @SuppressWarnings({"unchecked"})
        default M self() {
            return (M) this;
        }
    }

    abstract class AbstractDescriptor<T> implements Descriptor<T> {

        protected final String name;
        protected final T      target;

        protected AbstractDescriptor(String name, T target) {
            this.name = name;
            this.target = target;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public T unwrap() {
            return target;
        }

    }

    abstract class AbstractMutableDescriptor<M extends Mutable<M, T, D>, T, D extends Descriptor<T>>
            implements Mutable<M, T, D> {

        protected String name;
        protected T      target;

        protected AbstractMutableDescriptor(T target) {
            this.target = target;
        }

        @Override
        public M name(String name) {
            this.name = name;
            return self();
        }

        @Override
        public M name() {
            return name(target.getClass().getSimpleName());
        }

    }
}
