package org.jmouse.beans;

/**
 * A functional interface representing a factory for creating objects.
 * <p>
 * This interface is typically used in contexts where an bean needs to be
 * lazily instantiated or dynamically created, such as in dependency injection
 * frameworks or for managing scoped beans.
 * </p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * ObjectFactory<MyService> factory = () -> new MyService();
 * MyService service = factory.createObject();
 * }</pre>
 *
 * @param <T> the type of bean created by this factory
 */
@FunctionalInterface
public interface ObjectFactory<T> {

    /**
     * Creates a new instance of the bean.
     *
     * @return a newly created instance of {@code T}
     */
    T createObject();
}
