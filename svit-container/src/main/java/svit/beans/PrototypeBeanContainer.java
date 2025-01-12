package svit.beans;

/**
 * A dummy container implementation of {@link BeanInstanceContainer} for prototype-scoped beans.
 * <p>
 * This container does not store or retrieve any beans. All its methods are effectively no-ops.
 */
public class PrototypeBeanContainer implements BeanInstanceContainer {

    /**
     * Retrieves a bean instance by its name.
     *
     * @param name the name of the bean to retrieve.
     * @param <T>  the type of the bean.
     * @return always {@code null}, as this container does not store beans.
     */
    @Override
    public <T> T getBean(String name) {
        return null;
    }

    /**
     * Registers a bean instance with the given name.
     * <p>
     * In this implementation, the method does nothing.
     *
     * @param name the name of the bean.
     * @param bean the bean instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {

    }

    /**
     * Checks whether a bean with the specified name is already registered in this container.
     *
     * @param name the name of the bean.
     * @return always {@code true} for current implementation.
     */
    @Override
    public boolean containsBean(String name) {
        return true;
    }
}
