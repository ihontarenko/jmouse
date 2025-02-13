package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.JavaType;

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
         * Constructs a new {@code ParameterDescriptor.PropertyDescriptorAccessor} instance.
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
     * Creates a descriptor for a method parameter with a specified depth for nested descriptor resolution.
     * <p>
     * This method generates a {@link ParameterDescriptor} for the given {@link Parameter},
     * resolving annotations and determining the type descriptor recursively up to the specified depth.
     * </p>
     *
     * @param parameter the parameter to describe
     * @param depth     the recursion depth limit for nested descriptor resolution
     * @return a {@link ParameterDescriptor} instance representing the parameter
     */
    static ParameterDescriptor forParameter(Parameter parameter, int depth) {
        return forParameter(parameter, null, depth);
    }

    /**
     * Creates a descriptor for a method parameter, allowing explicit type specification and depth control.
     * <p>
     * This method generates a {@link ParameterDescriptor} for the given {@link Parameter},
     * resolving its annotations and type descriptor. If a custom {@link JavaType} is provided,
     * it is used instead of deriving the type from the parameter itself.
     * </p>
     *
     * @param parameter     the parameter to describe
     * @param parameterType an explicit {@link JavaType} for the parameter, or {@code null} to infer from reflection
     * @param depth         the recursion depth limit for nested descriptor resolution
     * @return a {@link ParameterDescriptor} instance representing the parameter
     */
    static ParameterDescriptor forParameter(Parameter parameter, JavaType parameterType, int depth) {
        ParameterDescriptor.Builder builder = new ParameterDescriptor.Builder(parameter.getName());

        // Determine the parameter type, either provided explicitly or derived from reflection
        if (parameterType == null) {
            parameterType = JavaType.forParameter(parameter);
        }

        // Process and attach parameter annotations
        for (Annotation annotation : parameter.getAnnotations()) {
            builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
        }

        // Set the parameter type and internal representation
        builder.type(TypeDescriptor.forType(parameterType, depth - 1));
        builder.internal(parameter);

        return builder.build();
    }

}
