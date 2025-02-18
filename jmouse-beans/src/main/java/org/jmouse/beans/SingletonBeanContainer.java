package org.jmouse.beans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingletonBeanContainer implements BeanContainer {

    /**
     * A mapping of structured names to their instantiated structured instances.
     */
    private final Map<String, Object> instances = new ConcurrentHashMap<>();

    /**
     * Retrieves a structured instance by its name.
     *
     * @param name the name of the structured to retrieve.
     * @param <T>  the type of the structured.
     * @return the structured instance.
     */
    @Override
    public <T> T getBean(String name) {
        return (T) instances.get(name);
    }

    /**
     * Registers a structured instance with the given name.
     *
     * @param name the name of the structured.
     * @param bean the structured instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {
        if (bean == null) {
            throw new BeanContextException("Failed to register structured. Bean must be non NULL");
        }

        if (containsBean(name)) {
            throw new BeanContextException("Bean '%s' already present in container".formatted(name));
        }

        instances.put(name, bean);
    }

    /**
     * Checks whether a structured with the specified name is already registered in this container.
     *
     * @param name the name of the structured.
     * @return {@code true} if a structured with the given name exists, otherwise {@code false}.
     */
    @Override
    public boolean containsBean(String name) {
        return instances.containsKey(name);
    }

    @Override
    public String toString() {
        return "Singletons: " + instances.size();
    }

}
