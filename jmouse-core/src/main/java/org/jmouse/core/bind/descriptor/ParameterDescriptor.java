package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.ClassTypeInspector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a descriptor for a method or constructor parameter.
 * <p>
 * This interface extends {@link ElementDescriptor} and provides descriptor about the parameter,
 * including its type and annotations.
 * </p>
 *
 * @see ElementDescriptor
 * @see TypeDescriptor
 * @see AnnotationDescriptor
 */
public interface ParameterDescriptor extends ElementDescriptor<Parameter>, ClassTypeInspector {

    /**
     * Returns the type descriptor of this parameter.
     * <p>
     * This method provides access to the type descriptor representing the parameter's type.
     * </p>
     *
     * @return the {@link TypeDescriptor} representing the parameter type
     */
    TypeDescriptor getType();

    /**
     * Default implementation of {@link ParameterDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing descriptor related to parameters,
     * including their annotations and type.
     * </p>
     */
    class Implementation extends ElementDescriptor.Implementation<Parameter> implements ParameterDescriptor {

        private final TypeDescriptor type;

        /**
         * Constructs a new {@code ParameterDescriptor.Implementation} instance.
         *
         * @param name        the name of the parameter
         * @param internal    the underlying {@link Parameter} instance
         * @param annotations a set of annotation descriptors associated with this parameter
         * @param type        the descriptor representing the type of this parameter
         */
        Implementation(String name, Parameter internal, Set<AnnotationDescriptor> annotations, TypeDescriptor type) {
            super(name, internal, annotations);
            this.type = type;
        }

        /**
         * Returns the class type being inspected.
         *
         * @return the {@link Class} bean representing the inspected type
         */
        @Override
        public Class<?> getClassType() {
            return getType().getClassType();
        }

        /**
         * Returns the type descriptor of this parameter.
         *
         * @return the {@link TypeDescriptor} representing the parameter type
         */
        @Override
        public TypeDescriptor getType() {
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
     * This builder provides a fluent API for setting parameter descriptor before
     * creating an immutable {@link ParameterDescriptor} instance.
     * </p>
     */
    class Builder extends ElementDescriptor.Builder<Builder, Parameter, ParameterDescriptor> {

        private TypeDescriptor type;

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
         * @param type the {@link TypeDescriptor} representing the parameter type
         * @return this builder instance for method chaining
         */
        Builder type(TypeDescriptor type) {
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

    /**
     * Creates a descriptor for a method parameter.
     *
     * @param parameter the parameter to describe
     * @return a {@link ParameterDescriptor} instance representing the parameter
     */
    static ParameterDescriptor forParameter(Parameter parameter) {
        return forParameter(parameter, TypeDescriptor.DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a method parameter with a specified depth for nested elements.
     *
     * @param parameter the parameter to describe
     * @param depth     the recursion depth limit for nested descriptor resolution
     * @return a {@link ParameterDescriptor} instance
     */
    static ParameterDescriptor forParameter(Parameter parameter, int depth) {
        ParameterDescriptor.Builder builder = new ParameterDescriptor.Builder(parameter.getName());

        for (Annotation annotation : parameter.getAnnotations()) {
            builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
        }

        builder.type(TypeDescriptor.forClass(parameter.getType(), depth - 1));
        builder.internal(parameter);

        return builder.build();
    }
}
