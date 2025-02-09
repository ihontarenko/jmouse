package org.jmouse.core.metadata;

import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a descriptor for a method or constructor parameter.
 * <p>
 * This interface extends {@link ElementDescriptor} and provides metadata about the parameter,
 * including its type and annotations.
 * </p>
 *
 * @see ElementDescriptor
 * @see ClassDescriptor
 * @see AnnotationDescriptor
 */
public interface ParameterDescriptor extends ElementDescriptor<Parameter>, ClassTypeInspector {

    /**
     * Returns the type descriptor of this parameter.
     * <p>
     * This method provides access to the type descriptor representing the parameter's type.
     * </p>
     *
     * @return the {@link ClassDescriptor} representing the parameter type
     */
    ClassDescriptor getType();

    /**
     * Default implementation of {@link ParameterDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to parameters,
     * including their annotations and type.
     * </p>
     */
    class Implementation extends ElementDescriptor.Implementation<Parameter> implements ParameterDescriptor {

        private final ClassDescriptor type;

        /**
         * Constructs a new {@code ParameterDescriptor.Implementation} instance.
         *
         * @param name        the name of the parameter
         * @param internal    the underlying {@link Parameter} instance
         * @param annotations a set of annotation descriptors associated with this parameter
         * @param type        the descriptor representing the type of this parameter
         */
        Implementation(String name, Parameter internal, Set<AnnotationDescriptor> annotations, ClassDescriptor type) {
            super(name, internal, annotations);
            this.type = type;
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

        /**
         * Returns the type descriptor of this parameter.
         *
         * @return the {@link ClassDescriptor} representing the parameter type
         */
        @Override
        public ClassDescriptor getType() {
            return type;
        }

        @Override
        public String toString() {
            return "[%s] %s".formatted(getName(), type.toString());
        }
    }

    /**
     * A builder for constructing instances of {@link ParameterDescriptor}.
     * <p>
     * This builder provides a fluent API for setting parameter metadata before
     * creating an immutable {@link ParameterDescriptor} instance.
     * </p>
     */
    class Builder extends ElementDescriptor.Builder<Builder, Parameter, ParameterDescriptor> {

        private ClassDescriptor type;

        /**
         * Constructs a new {@code ParameterDescriptor.Builder} with the specified parameter name.
         *
         * @param name the name of the parameter
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the type of the parameter being built.
         *
         * @param type the {@link ClassDescriptor} representing the parameter type
         * @return this builder instance for method chaining
         */
        Builder type(ClassDescriptor type) {
            this.type = type;
            return self();
        }

        /**
         * Builds a new {@link ParameterDescriptor} instance using the configured values.
         * <p>
         * The resulting descriptor is immutable, ensuring thread-safety.
         * </p>
         *
         * @return a new immutable instance of {@link ParameterDescriptor}
         */
        @Override
        public ParameterDescriptor build() {
            return new Implementation(name, internal, Collections.unmodifiableSet(annotations), type);
        }
    }
}
