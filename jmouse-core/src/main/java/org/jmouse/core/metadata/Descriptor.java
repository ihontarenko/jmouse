package org.jmouse.core.metadata;

/**
 * A base interface for describing various elements within the Descriptive API.
 * <p>
 * This interface provides a common structure for descriptors that represent different
 * elements, such as types, methods, fields, properties, constructors, and annotations.
 * Implementations of this interface allow accessing metadata and internal representations
 * of the described elements.
 * </p>
 *
 * @see ElementDescriptor
 * @see ClassDescriptor
 * @see BeanDescriptor
 * @see MethodDescriptor
 * @see FieldDescriptor
 * @see PropertyDescriptor
 * @see ConstructorDescriptor
 * @see ParameterDescriptor
 * @see AnnotationDescriptor
 * @see EnumConstantDescriptor
 */
public interface Descriptor<T> {

    String getName();

    T getInternal();

    interface Builder<B extends Builder<B, I, D>, I, D extends Descriptor<I>> {

        B name(String name);

        B internal(I internal);

        D build();

        @SuppressWarnings({"unchecked"})
        default B self() {
            return (B) this;
        }

    }

    abstract class Implementation<T> implements Descriptor<T> {

        protected final String name;
        protected final T internal;

        protected Implementation(String name, T internal) {
            this.name = name;
            this.internal = internal;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public T getInternal() {
            return internal;
        }

    }

    abstract class DescriptorBuilder<B extends Builder<B, I, D>, I, D extends Descriptor<I>>
            implements Descriptor.Builder<B, I, D> {

        protected String name;
        protected I internal;

        protected DescriptorBuilder(String name) {
            this.name = name;
        }

        @Override
        public B name(String name) {
            this.name = name;
            return self();
        }

        @Override
        public B internal(I internal) {
            this.internal = internal;
            return self();
        }

    }

}
