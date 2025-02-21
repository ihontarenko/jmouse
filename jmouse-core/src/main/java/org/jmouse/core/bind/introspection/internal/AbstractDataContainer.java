package org.jmouse.core.bind.introspection.internal;

/**
 * A base implementation of {@link DataContainer} that provides common functionality
 * for managing a data target and its associated name.
 * <p>
 * This class serves as a foundation for more specific data container implementations.
 * </p>
 *
 * @param <T> the type of the target data stored in this container
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class AbstractDataContainer<T> implements DataContainer<T> {

    private T      target;
    private String name;

    /**
     * Creates a new instance of {@code AbstractDataContainer} with the specified target.
     *
     * @param target the initial target object
     */
    public AbstractDataContainer(T target) {
        this.target = target;
    }

    /**
     * Retrieves the name associated with this container.
     *
     * @return the name of this container, or {@code null} if not set
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of this container.
     *
     * @param name the name to assign
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the target object stored in this container.
     *
     * @return the target object
     */
    @Override
    public T getTarget() {
        return this.target;
    }

    /**
     * Sets a new target object for this container.
     *
     * @param target the new target object
     */
    @Override
    public void setTarget(T target) {
        this.target = target;
    }

    /**
     * Returns a string representation of this container.
     *
     * @return a string in the format {@code [name]: target}
     */
    @Override
    public String toString() {
        return "[%s]: %s".formatted(name, target);
    }
}
