package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.bean.JavaBeanPropertyDescriptor;

/**
 * A base interface for describing various elements within the Descriptive API.
 * <p>
 * This interface provides a common structure for descriptors that represent different
 * elements, such as types, methods, fields, properties, constructors, and annotations.
 * Implementations of this interface allow accessing descriptor and internal representations
 * of the described elements.
 * </p>
 *
 * @param <T> the type of the internal representation of the described element
 * @see ElementDescriptor
 * @see TypeDescriptor
 * @see JavaBeanDescriptor
 * @see MethodDescriptor
 * @see FieldDescriptor
 * @see JavaBeanPropertyDescriptor
 * @see ConstructorDescriptor
 * @see ParameterDescriptor
 * @see AnnotationDescriptor
 */
public interface Descriptor<T> {

    /**
     * Returns the name associated with this descriptor.
     * <p>
     * The name is typically used to identify the described element.
     *
     * @return the name of this descriptor
     */
    String getName();

    /**
     * Returns the internal representation of this descriptor.
     * <p>
     * The returned bean depends on the implementation. It could be a {@link Class},
     * a {@link java.lang.reflect.Method}, a {@link java.lang.reflect.Field}, or any other
     * representation of the described element.
     *
     * @return the internal representation of this descriptor
     */
    T getInternal();

    /**
     * A builder interface for constructing instances of {@link Descriptor}.
     * <p>
     * This interface defines a fluent API for setting descriptor properties before
     * finalizing the instance.
     * </p>
     *
     * @param <B> the type of the builder itself, used for fluent API methods
     * @param <I> the type of the internal representation of the element
     * @param <D> the type of the descriptor being built
     */
    interface Builder<B extends Builder<B, I, D>, I, D extends Descriptor<I>> {

        /**
         * Sets the name of the descriptor being built.
         *
         * @param name the name to assign
         * @return this builder instance for method chaining
         */
        B name(String name);

        /**
         * Sets the internal representation of the descriptor being built.
         *
         * @param internal the internal bean to assign
         * @return this builder instance for method chaining
         */
        B internal(I internal);

        /**
         * Constructs the final descriptor instance.
         *
         * @return a new instance of {@code D}
         */
        D build();

        /**
         * Returns the builder instance itself, ensuring proper type inference for method chaining.
         *
         * @return this builder instance
         */
        @SuppressWarnings({"unchecked"})
        default B self() {
            return (B) this;
        }
    }

    /**
     * A base implementation of the {@link Descriptor} interface.
     * <p>
     * This abstract class provides default implementations of {@link #getName()}
     * and {@link #getInternal()}.
     * </p>
     *
     * @param <T> the type of the internal representation of the described element
     */
    abstract class Implementation<T> implements Descriptor<T> {

        protected final String name;
        protected final T internal;

        /**
         * Constructs a new {@code Descriptor.Implementation} with the given parameters.
         *
         * @param name     the name of the element
         * @param internal the internal representation of the element
         */
        protected Implementation(String name, T internal) {
            this.name = name;
            this.internal = internal;
        }

        /**
         * Returns the name associated with this descriptor.
         *
         * @return the name of this descriptor
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Returns the internal representation of this descriptor.
         *
         * @return the internal representation of this descriptor
         */
        @Override
        public T getInternal() {
            return internal;
        }
    }

    /**
     * A base builder implementation for constructing {@link Descriptor} instances.
     * <p>
     * This abstract class provides common functionality for descriptor builders,
     * ensuring consistency across different descriptor types.
     * </p>
     *
     * @param <B> the type of the builder itself, used for fluent API methods
     * @param <I> the type of the internal representation of the element
     * @param <D> the type of the descriptor being built
     */
    abstract class DescriptorBuilder<B extends Builder<B, I, D>, I, D extends Descriptor<I>>
            implements Descriptor.Builder<B, I, D> {

        protected String name;
        protected I      internal;

        /**
         * Constructs a new {@code DescriptorBuilder} with the given name.
         *
         * @param name the name of the element being built
         */
        protected DescriptorBuilder(String name) {
            this.name = name;
        }

        /**
         * Sets the name of the descriptor being built.
         *
         * @param name the name to assign
         * @return this builder instance for method chaining
         */
        @Override
        public B name(String name) {
            this.name = name;
            return self();
        }

        /**
         * Sets the internal representation of the descriptor being built.
         *
         * @param internal the internal bean to assign
         * @return this builder instance for method chaining
         */
        @Override
        public B internal(I internal) {
            this.internal = internal;
            return self();
        }
    }
}
