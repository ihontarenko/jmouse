package org.jmouse.core.bind.descriptor;

import java.lang.reflect.Executable;
import java.util.*;

/**
 * Represents a descriptor for executable elements such as methods and constructors.
 * <p>
 * This interface extends {@link ElementDescriptor} and provides access to parameter
 * descriptors and exception types associated with the executable element.
 * </p>
 *
 * @param <E> the type of {@link Executable} being described (e.g., {@link java.lang.reflect.Method} or {@link ConstructorDescriptor})
 * @see MethodDescriptor
 * @see ConstructorDescriptor
 * @see ParameterDescriptor
 * @see TypeDescriptor
 */
public interface ExecutableDescriptor<E extends Executable> extends ElementDescriptor<E> {

    /**
     * Returns a collection of parameter descriptors associated with this executable.
     * <p>
     * This collection represents the parameters declared in the method or constructor.
     * Each parameter is described using a {@link ParameterDescriptor}.
     * </p>
     *
     * @return a collection of {@link ParameterDescriptor} instances representing the parameters
     */
    Collection<ParameterDescriptor> getParameters();

    /**
     * Retrieves the parameter descriptor at the specified index.
     * <p>
     * If the index is out of range, this method returns {@code null}.
     * </p>
     *
     * @param index the index of the parameter (zero-based)
     * @return the {@link ParameterDescriptor} at the given index, or {@code null} if out of bounds
     */
    default ParameterDescriptor getParameter(int index) {
        ParameterDescriptor descriptor = null;

        if (index >= 0 && index < getParameters().size()) {
            descriptor = List.copyOf(getParameters()).get(index);
        }

        return descriptor;
    }

    /**
     * Returns a collection of exception types that this executable can throw.
     * <p>
     * The returned collection contains descriptors of the declared exceptions.
     * </p>
     *
     * @return a collection of {@link TypeDescriptor} instances representing exception types
     */
    Collection<TypeDescriptor> getExceptionTypes();

    /**
     * Retrieves the exception type descriptor at the specified index.
     * <p>
     * If the index is out of range, this method returns {@code null}.
     * </p>
     *
     * @param index the index of the exception type (zero-based)
     * @return the {@link TypeDescriptor} at the given index, or {@code null} if out of bounds
     */
    default TypeDescriptor getExceptionType(int index) {
        TypeDescriptor exceptionType = null;

        if (index >= 0 && index < getExceptionTypes().size()) {
            exceptionType = List.copyOf(getExceptionTypes()).get(index);
        }

        return exceptionType;
    }


    /**
     * A default implementation of {@link ExecutableDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing descriptor related to executable elements,
     * including their annotations, parameters, and declared exception types.
     * </p>
     *
     * @param <E> the type of {@link Executable} being described
     */
    abstract class Implementation<E extends Executable>
            extends ElementDescriptor.Implementation<E> implements ExecutableDescriptor<E> {

        protected final Collection<ParameterDescriptor> parameters;
        protected final Collection<TypeDescriptor>      exceptionTypes;

        /**
         * Constructs a new {@code ExecutableDescriptor.PropertyDescriptorAccessor} instance.
         *
         * @param name          the name of the executable (e.g., method or constructor name)
         * @param internal      the underlying {@link Executable} instance
         * @param annotations   a set of annotation descriptors associated with this executable
         * @param parameters    a collection of parameter descriptors representing the executable's parameters
         * @param exceptionTypes a collection of class descriptors representing the declared exception types
         */
        public Implementation(
                String name, E internal,
                Set<AnnotationDescriptor> annotations,
                Collection<ParameterDescriptor> parameters,
                Collection<TypeDescriptor> exceptionTypes
        ) {
            super(name, internal, annotations);
            this.parameters = parameters;
            this.exceptionTypes = exceptionTypes;
        }

        /**
         * Returns a collection of parameter descriptors associated with this executable.
         *
         * @return a collection of {@link ParameterDescriptor} instances
         */
        @Override
        public Collection<ParameterDescriptor> getParameters() {
            return parameters;
        }

        /**
         * Returns a collection of exception types that this executable can throw.
         *
         * @return a collection of {@link TypeDescriptor} instances representing exception types
         */
        @Override
        public Collection<TypeDescriptor> getExceptionTypes() {
            return exceptionTypes;
        }
    }

    /**
     * A builder for constructing instances of {@link ExecutableDescriptor}.
     * <p>
     * This abstract builder provides methods for setting parameters and exception types
     * before finalizing the descriptor instance.
     * </p>
     *
     * @param <B> the type of the builder itself, used for fluent API methods
     * @param <E> the type of {@link Executable} being described
     * @param <D> the type of {@link ExecutableDescriptor} being built
     */
    abstract class Builder<B extends Builder<B, E, D>, E extends Executable, D extends ExecutableDescriptor<E>>
            extends ElementDescriptor.Builder<B, E, D> {

        protected Set<ParameterDescriptor> parameters     = new HashSet<>();
        protected Set<TypeDescriptor>      exceptionTypes = new HashSet<>();

        /**
         * Constructs a new {@code ExecutableDescriptor.Builder} with the specified executable name.
         *
         * @param name the name of the executable (e.g., method or constructor name)
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the parameters of the executable.
         *
         * @param parameter a {@link ParameterDescriptor} instance
         * @return this builder instance for method chaining
         */
        public B parameter(ParameterDescriptor parameter) {
            this.parameters.add(parameter);
            return self();
        }

        /**
         * Sets the exception types of the executable using a varargs array.
         *
         * @param exceptionType an array of {@link TypeDescriptor} instances
         * @return this builder instance for method chaining
         */
        public B exceptionType(TypeDescriptor exceptionType) {
            this.exceptionTypes.add(exceptionType);
            return self();
        }
    }
}
