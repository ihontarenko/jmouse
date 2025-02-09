package org.jmouse.core.metadata;

import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
 * @see ClassDescriptor
 */
public interface ExecutableDescriptor<E extends Executable> extends ElementDescriptor<E> {

    /**
     * Returns a collection of parameter descriptors associated with this executable.
     * <p>
     * This collection represents the parameters declared in the method or constructor.
     * </p>
     *
     * @return a collection of {@link ParameterDescriptor} instances
     */
    Collection<ParameterDescriptor> getParameters();

    /**
     * Returns a collection of exception types that this executable can throw.
     * <p>
     * The returned collection contains descriptors of the declared exceptions.
     * </p>
     *
     * @return a collection of {@link ClassDescriptor} instances representing exception types
     */
    Collection<ClassDescriptor> getExceptionTypes();

    /**
     * A default implementation of {@link ExecutableDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to executable elements,
     * including their annotations, parameters, and declared exception types.
     * </p>
     *
     * @param <E> the type of {@link Executable} being described
     */
    abstract class Implementation<E extends Executable>
            extends ElementDescriptor.Implementation<E> implements ExecutableDescriptor<E> {

        protected final Collection<ParameterDescriptor> parameters;
        protected final Collection<ClassDescriptor>     exceptionTypes;

        /**
         * Constructs a new {@code ExecutableDescriptor.Implementation} instance.
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
                Collection<ClassDescriptor> exceptionTypes
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
         * @return a collection of {@link ClassDescriptor} instances representing exception types
         */
        @Override
        public Collection<ClassDescriptor> getExceptionTypes() {
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
        protected Set<ClassDescriptor>     exceptionTypes = new HashSet<>();

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
         * @param exceptionTypes an array of {@link ClassDescriptor} instances
         * @return this builder instance for method chaining
         */
        public B exceptionType(ClassDescriptor exceptionType) {
            this.exceptionTypes.add(exceptionType);
            return self();
        }
    }
}
