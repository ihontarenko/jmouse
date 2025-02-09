package org.jmouse.core.metadata;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a descriptor for a {@link Constructor}, providing metadata such as annotations,
 * parameters, and declared exception types.
 * <p>
 * This interface extends {@link ExecutableDescriptor} to specialize it for constructors.
 * </p>
 *
 * @see ExecutableDescriptor
 * @see ParameterDescriptor
 * @see AnnotationDescriptor
 * @see ClassDescriptor
 */
public interface ConstructorDescriptor extends ExecutableDescriptor<Constructor<?>> {

    /**
     * Default implementation of {@link ConstructorDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing metadata related to constructors,
     * including their annotations, parameters, and declared exception types.
     * </p>
     */
    class Implementation extends ExecutableDescriptor.Implementation<Constructor<?>> implements ConstructorDescriptor {

        /**
         * Constructs a new {@code ConstructorDescriptor.Implementation} instance.
         *
         * @param name          the name of the constructor (typically the class name)
         * @param internal      the underlying {@link Constructor} instance
         * @param annotations   a set of annotation descriptors associated with this constructor
         * @param parameters    a collection of parameter descriptors representing constructor parameters
         * @param exceptionTypes a collection of class descriptors representing declared exception types
         */
        Implementation(
                String name, Constructor<?> internal,
                Set<AnnotationDescriptor> annotations,
                Collection<ParameterDescriptor> parameters,
                Collection<ClassDescriptor> exceptionTypes
        ) {
            super(name, internal, annotations, parameters, exceptionTypes);
        }

    }

    /**
     * A builder for constructing instances of {@link ConstructorDescriptor}.
     * <p>
     * This builder provides a fluent API for setting constructor metadata before
     * creating an immutable {@link ConstructorDescriptor} instance.
     * </p>
     */
    class Builder extends ExecutableDescriptor.Builder<Builder, Constructor<?>, ConstructorDescriptor> {

        /**
         * Constructs a new {@code ConstructorDescriptor.Builder} with the specified constructor name.
         *
         * @param name the name of the constructor (typically the class name)
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Builds a new {@link ConstructorDescriptor} instance using the configured values.
         * <p>
         * The resulting descriptor is immutable, ensuring thread-safety.
         * </p>
         *
         * @return a new immutable instance of {@link ConstructorDescriptor}
         */
        @Override
        public ConstructorDescriptor build() {
            return new Implementation(
                    name,
                    internal,
                    Collections.unmodifiableSet(annotations),
                    Collections.unmodifiableSet(parameters),
                    Collections.unmodifiableSet(exceptionTypes)
            );
        }
    }
}
