package svit.container;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingletonBeanContainer implements BeanInstanceContainer {

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
        instances.put(name, bean);
    }

}
