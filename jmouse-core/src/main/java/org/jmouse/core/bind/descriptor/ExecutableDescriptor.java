package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.ExecutableData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a descriptor for an executable element, such as a method or constructor.
 * This class provides metadata descriptor capabilities for executable elements,
 * including parameter descriptors and exception types.
 *
 * @param <E> the type of the executable element (e.g., {@link java.lang.reflect.Method} or {@link Constructor})
 * @param <C> the type of the associated container holding metadata
 * @param <I> the type of the introspector responsible for processing metadata
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 */
abstract public class ExecutableDescriptor<E extends Executable, C extends ExecutableData<E>, I extends ExecutableIntrospector<?, ?, ?, ?>>
        extends AnnotatedElementDescriptor<E, C, I> {

    /**
     * Constructs an {@code ExecutableDescriptor} with the given introspector and container.
     *
     * @param introspector the introspector responsible for analyzing the executable element
     * @param container    the container holding metadata of the executable element
     */
    protected ExecutableDescriptor(I introspector, C container) {
        super(introspector, container);
    }

    /**
     * Retrieves the parameters of the executable element.
     * If parameters are not initialized, they will be introspected.
     *
     * @return an unmodifiable collection of {@link ParameterDescriptor}
     */
    public Collection<ParameterDescriptor> getParameters() {
        Collection<ParameterDescriptor> parameters = container.getParameters();

        if (parameters.isEmpty()) {
            toIntrospector().parameters();
            parameters = container.getParameters();
        }

        return Collections.unmodifiableCollection(parameters);
    }

    /**
     * Retrieves a parameter descriptor by its name.
     *
     * @param name the name of the parameter
     * @return the {@link ParameterDescriptor} if found, otherwise {@code null}
     */
    public ParameterDescriptor getParameter(String name) {
        return container.getParameter(name);
    }

    /**
     * Retrieves a parameter descriptor by its index.
     *
     * @param index the zero-based index of the parameter
     * @return the {@link ParameterDescriptor} if found, otherwise {@code null}
     */
    public ParameterDescriptor getParameter(int index) {
        ParameterDescriptor descriptor = null;

        if (index >= 0 && index < getParameters().size()) {
            descriptor = List.copyOf(getParameters()).get(index);
        }

        return descriptor;
    }

    /**
     * Retrieves the list of exception types declared by the executable element.
     *
     * @return an unmodifiable list of {@link ClassTypeDescriptor} representing exception types
     */
    public List<ClassTypeDescriptor> getExceptionTypes() {
        return Collections.unmodifiableList(container.getExceptionTypes());
    }

    /**
     * Returns a string representation of the executable descriptor, indicating whether
     * it is a constructor ('C') or a method ('M') followed by its name.
     *
     * @return a formatted string representation of this descriptor
     */
    @Override
    public String toString() {
        return "[%s]: %s".formatted(container.getTarget() instanceof Constructor<?> ? "C" : "M", getName());
    }
}
