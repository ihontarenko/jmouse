package org.jmouse.beans;

/**
 * A dummy container implementation of {@link BeanContainer} for prototype-scoped beans.
 * <p>
 * This container does not store or retrieve any beans. All its methods are effectively no-ops.
 */
public class PrototypeBeanContainer implements BeanContainer {

    /**
     * Retrieves a structured instance by its name.
     *
     * @param name the name of the structured to retrieve.
     * @param <T>  the type of the structured.
     * @return always {@code null}, as this container does not store beans.
     */
    @Override
    public <T> T getBean(String name) {
        return null;
    }

    /**
     * Registers a structured instance with the given name.
     * <p>
     * In this implementation, the method does nothing.
     *
     * @param name the name of the structured.
     * @param bean the structured instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {

    }

    /**
     * Checks whether a structured with the specified name is already registered in this container.
     *
     * @param name the name of the structured.
     * @return always {@code true} for current implementation.
     */
    @Override
    public boolean containsBean(String name) {
        return true;
    }

    @Override
    public String toString() {
        return "Prototypes: -1";
    }
}
