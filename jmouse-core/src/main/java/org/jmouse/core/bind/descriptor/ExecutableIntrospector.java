package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.ExecutableData;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

/**
 * Provides an abstraction for introspecting {@link Executable} elements, including
 * methods and constructors. This class is responsible for analyzing metadata such as
 * annotations, parameters, and method names.
 * <p>
 * Implementations of this class allow extracting structured metadata about
 * executable elements for further processing.
 * </p>
 *
 * @param <C> the type of container holding executable metadata
 * @param <I> the specific implementation type of {@code ExecutableIntrospector} (used for fluent API)
 * @param <E> the type of the executable element being introspected (e.g., {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor})
 * @param <D> the type of the descriptor generated from descriptor
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
abstract public class ExecutableIntrospector<
        C extends ExecutableData<E>,
        I extends ExecutableIntrospector<C, I, E, D>,
        E extends Executable,
        D extends ExecutableDescriptor<E, C, I>>
        extends AnnotatedElementIntrospector<C, I, E, D> {

    /**
     * Constructs an {@code ExecutableIntrospector} with the given executable target.
     *
     * @param target the executable element to introspect (e.g., method or constructor)
     */
    protected ExecutableIntrospector(E target) {
        super(target);
    }

    /**
     * Sets the name of the executable based on its reflective definition.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public I name() {
        return name(container.getTarget().getName());
    }

    /**
     * Adds a parameter descriptor to this executable introspector.
     *
     * @param descriptor the parameter descriptor to add
     * @return this introspector instance for method chaining
     */
    public I parameter(ParameterDescriptor descriptor) {
        container.addParameter(descriptor);
        return self();
    }

    /**
     * Extracts and processes parameter descriptors for all parameters of this executable.
     * <p>
     * Each parameter is introspected and added as a {@link ParameterDescriptor}.
     * </p>
     *
     * @return this introspector instance for method chaining
     */
    public I parameters() {
        Parameter[] parameters = container.getTarget().getParameters();

        for (Parameter parameter : parameters) {
            parameter(new ParameterIntrospector(parameter).introspect().toDescriptor());
        }

        return self();
    }

    /**
     * Performs descriptor by analyzing the name and annotations of this executable.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public I introspect() {
        name().annotations();
        return self();
    }
}
