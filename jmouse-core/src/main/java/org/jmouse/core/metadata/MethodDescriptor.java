package org.jmouse.core.metadata;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a descriptor for a {@link Method}, providing metadata such as return type,
 * annotations, parameters, and declared exceptions.
 * <p>
 * This interface extends {@link ExecutableDescriptor} to specialize it for method descriptions.
 * </p>
 *
 * @see ExecutableDescriptor
 * @see ParameterDescriptor
 * @see AnnotationDescriptor
 * @see ClassDescriptor
 */
public interface MethodDescriptor extends ExecutableDescriptor<Method> {

    /**
     * Returns the return type descriptor of this method.
     * <p>
     * This method provides access to the type descriptor representing the return type
     * of the method.
     * </p>
     *
     * @return the {@link ClassDescriptor} representing the return type of this method
     */
    ClassDescriptor getReturnType();

    /**
     * Default implementation of {@link MethodDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to methods,
     * including their annotations, parameters, return type, and declared exception types.
     * </p>
     */
    class Implementation extends ExecutableDescriptor.Implementation<Method> implements MethodDescriptor {

        protected final ClassDescriptor returnType;

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
                Collection<ClassDescriptor> exceptionTypes,
                ClassDescriptor returnType
        ) {
            super(name, internal, annotations, parameters, exceptionTypes);
            this.returnType = returnType;
        }

        /**
         * Returns the return type descriptor of this method.
         *
         * @return the {@link ClassDescriptor} representing the return type of this method
         */
        @Override
        public ClassDescriptor getReturnType() {
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
     * This builder provides a fluent API for setting method metadata before
     * creating an immutable {@link MethodDescriptor} instance.
     * </p>
     */
    class Builder extends ExecutableDescriptor.Builder<Builder, Method, MethodDescriptor> {

        private ClassDescriptor returnType;

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
         * @param returnType the {@link ClassDescriptor} representing the return type
         * @return this builder instance for method chaining
         */
        public Builder returnType(ClassDescriptor returnType) {
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
}
