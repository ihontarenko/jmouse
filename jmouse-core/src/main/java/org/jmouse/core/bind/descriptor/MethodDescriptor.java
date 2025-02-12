package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.jmouse.core.bind.descriptor.TypeDescriptor.forClass;

/**
 * Represents a descriptor for a {@link Method}, providing descriptor such as return type,
 * annotations, parameters, and declared exceptions.
 * <p>
 * This interface extends {@link ExecutableDescriptor} to specialize it for method descriptions.
 * </p>
 *
 * @see ExecutableDescriptor
 * @see ParameterDescriptor
 * @see AnnotationDescriptor
 * @see TypeDescriptor
 */
public interface MethodDescriptor extends ExecutableDescriptor<Method> {

    /**
     * Returns the return type descriptor of this method.
     * <p>
     * This method provides access to the type descriptor representing the return type
     * of the method.
     * </p>
     *
     * @return the {@link TypeDescriptor} representing the return type of this method
     */
    TypeDescriptor getReturnType();

    /**
     * Determines if this method is a JavaBean-style setter.
     * <p>
     * A setter method follows the convention of having a name prefixed with
     * {@code "set"} and accepting a single parameter.
     * </p>
     *
     * @return {@code true} if this method is a setter, otherwise {@code false}
     */
    default boolean isSetter() {
        return MethodMatchers.setter().matches(getInternal());
    }

    /**
     * Determines if this method is a JavaBean-style getter.
     * <p>
     * A getter method follows the convention of having a name prefixed with
     * {@code "get"} or {@code "is"} and returning a value.
     * </p>
     *
     * @return {@code true} if this method is a getter, otherwise {@code false}
     */
    default boolean isGetter() {
        return MethodMatchers.getter().matches(getInternal());
    }

    /**
     * Returns a {@link Setter} function based on this method if it is a setter.
     * <p>
     * This method constructs a {@link Setter} instance that allows setting values
     * on an bean via this method if it follows the setter convention.
     * </p>
     *
     * @param <T> the type of the bean containing the method
     * @param <V> the type of the value being set
     * @return a {@link Setter} instance for this method
     * @throws IllegalArgumentException if the method is not a setter
     */
    default <T, V> Setter<T, V> getSetter() {
        return Setter.ofMethod(getInternal());
    }

    /**
     * Returns a {@link Getter} function based on this method if it is a getter.
     * <p>
     * This method constructs a {@link Getter} instance that allows retrieving values
     * from an bean via this method if it follows the getter convention.
     * </p>
     *
     * @param <T> the type of the bean containing the method
     * @param <V> the return type of the method
     * @return a {@link Getter} instance for this method
     * @throws IllegalArgumentException if the method is not a getter
     */
    default <T, V> Getter<T, V> getGetter() {
        return Getter.ofMethod(getInternal());
    }

    /**
     * Default implementation of {@link MethodDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing descriptor related to methods,
     * including their annotations, parameters, return type, and declared exception types.
     * </p>
     */
    class Implementation extends ExecutableDescriptor.Implementation<Method> implements MethodDescriptor {

        protected final TypeDescriptor returnType;

        /**
         * Constructs a new {@code MethodDescriptor.Implementation} instance.
         *
         * @param name          the name of the method
         * @param internal      the underlying {@link Method} instance
         * @param annotations   a set of annotation descriptors associated with this method
         * @param parameters    a collection of parameter descriptors representing the method parameters
         * @param exceptionTypes a collection of class descriptors representing the declared exception types
         * @param returnType    the descriptor representing the return type of this method
         */
        Implementation(
                String name, Method internal,
                Set<AnnotationDescriptor> annotations,
                Collection<ParameterDescriptor> parameters,
                Collection<TypeDescriptor> exceptionTypes,
                TypeDescriptor returnType
        ) {
            super(name, internal, annotations, parameters, exceptionTypes);
            this.returnType = returnType;
        }

        /**
         * Returns the return type descriptor of this method.
         *
         * @return the {@link TypeDescriptor} representing the return type of this method
         */
        @Override
        public TypeDescriptor getReturnType() {
            return returnType;
        }

        /**
         * Returns a string representation of this method descriptor.
         * <p>
         * The format used is {@code "methodName : returnType"}.
         * </p>
         *
         * @return a string representation of this method descriptor
         */
        @Override
        public String toString() {
            return "%s : %s".formatted(Reflections.getMethodName(internal), returnType);
        }
    }

    /**
     * A builder for constructing instances of {@link MethodDescriptor}.
     * <p>
     * This builder provides a fluent API for setting method descriptor before
     * creating an immutable {@link MethodDescriptor} instance.
     * </p>
     */
    class Builder extends ExecutableDescriptor.Builder<Builder, Method, MethodDescriptor> {

        private TypeDescriptor returnType;

        /**
         * Constructs a new {@code MethodDescriptor.Builder} with the specified method name.
         *
         * @param name the name of the method
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the return type of the method being built.
         *
         * @param returnType the {@link TypeDescriptor} representing the return type
         * @return this builder instance for method chaining
         */
        public Builder returnType(TypeDescriptor returnType) {
            this.returnType = returnType;
            return self();
        }

        /**
         * Builds a new {@link MethodDescriptor} instance using the configured values.
         * <p>
         * The resulting descriptor is immutable, ensuring thread-safety.
         * </p>
         *
         * @return a new immutable instance of {@link MethodDescriptor}
         */
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

    /**
     * Creates a descriptor for a method.
     *
     * @param method the method to describe
     * @return a {@link MethodDescriptor} instance representing the method
     */
    static MethodDescriptor forMethod(Method method) {
        return forMethod(method, TypeDescriptor.DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a method with a specified depth for nested elements.
     *
     * @param method the method to describe
     * @param depth  the recursion depth limit for nested descriptor resolution
     * @return a {@link MethodDescriptor} instance
     */
    @SuppressWarnings("all")
    static MethodDescriptor forMethod(Method method, int depth) {
        MethodDescriptor.Builder builder = new MethodDescriptor.Builder(method.getName());

        for (Parameter parameter : method.getParameters()) {
            builder.parameter(ParameterDescriptor.forParameter(parameter, depth - 1));
        }

        for (Class<?> exceptionType : method.getExceptionTypes()) {
            builder.exceptionType(forClass(exceptionType, depth - 1));
        }

        for (Annotation annotation : method.getAnnotations()) {
            builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
        }

        return builder.internal(method).returnType(forClass(method.getReturnType(), depth - 1)).build();
    }
}
