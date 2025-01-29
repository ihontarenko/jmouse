package svit.beans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingletonBeanContainer implements BeanContainer {

    /**
     * A mapping of bean names to their instantiated bean instances.
     */
    private final Map<String, Object> instances = new ConcurrentHashMap<>();

    /**
     * Retrieves a bean instance by its name.
     *
     * @param name the name of the bean to retrieve.
     * @param <T>  the type of the bean.
     * @return the bean instance.
     */
    @Override
    public <T> T getBean(String name) {
        return (T) instances.get(name);
    }

    /**
     * Registers a bean instance with the given name.
     *
     * @param name the name of the bean.
     * @param bean the bean instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {
        if (bean == null) {
            throw new BeanContextException("Failed to register bean. Bean must be non NULL");
        }

        if (containsBean(name)) {
            throw new BeanContextException("Bean '%s' already present in container".formatted(name));
        }

        instances.put(name, bean);
    }

    /**
     * Checks whether a bean with the specified name is already registered in this container.
     *
     * @param name the name of the bean.
     * @return {@code true} if a bean with the given name exists, otherwise {@code false}.
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
