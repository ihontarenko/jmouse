package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a descriptor for a {@link Constructor}, providing descriptor such as annotations,
 * parameters, and declared exception types.
 * <p>
 * This interface extends {@link ExecutableDescriptor} to specialize it for constructors.
 * </p>
 *
 * @see ExecutableDescriptor
 * @see ParameterDescriptor
 * @see AnnotationDescriptor
 * @see TypeDescriptor
 */
public interface ConstructorDescriptor extends ExecutableDescriptor<Constructor<?>> {

    /**
     * Default implementation of {@link ConstructorDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing descriptor related to constructors,
     * including their annotations, parameters, and declared exception types.
     * </p>
     */
    class Implementation extends ExecutableDescriptor.Implementation<Constructor<?>> implements ConstructorDescriptor {

        /**
         * Constructs a new {@code ConstructorDescriptor.PropertyDescriptorAccessor} instance.
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
                Collection<TypeDescriptor> exceptionTypes
        ) {
            super(name, internal, annotations, parameters, exceptionTypes);
        }

    }

    /**
     * A builder for constructing instances of {@link ConstructorDescriptor}.
     * <p>
     * This builder provides a fluent API for setting constructor descriptor before
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

    /**
     * Creates a descriptor for a constructor.
     *
     * @param constructor the constructor to describe
     * @return a {@link ConstructorDescriptor} instance representing the constructor
     */
    static ConstructorDescriptor forContructor(Constructor<?> constructor) {
        return forContructor(constructor, TypeDescriptor.DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a constructor with a specified depth for nested elements.
     *
     * @param constructor the constructor to describe
     * @param depth       the recursion depth limit for nested descriptor resolution
     * @return a {@link ConstructorDescriptor} instance
     */
    @SuppressWarnings("all")
    static ConstructorDescriptor forContructor(Constructor<?> constructor, int depth) {
        ConstructorDescriptor.Builder builder = new ConstructorDescriptor.Builder(constructor.getName());

        int         parametersCount = constructor.getParameterCount();
        Parameter[] parameters      = constructor.getParameters();
        for (int i = 0; i < parametersCount; i++) {
            JavaType parameterType = JavaType.forParameter(constructor, i);
            builder.parameter(ParameterDescriptor.forParameter(parameters[i], parameterType, depth - 1));
        }

        int exceptionTypes = constructor.getExceptionTypes().length;
        for (int i = 0; i < exceptionTypes; i++) {
            builder.exceptionType(TypeDescriptor.forType(JavaType.forExceptionType(constructor, i), depth - 1));
        }

        for (Annotation annotation : constructor.getAnnotations()) {
            builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
        }

        return builder.internal(constructor).build();
    }
}
