package svit.beans;

/**
 * A bean container implementation that delegates all operations to another {@link BeanContainer}.
 * <p>
 * This class acts as a proxy to another bean container, forwarding all calls to the delegate instance.
 * Useful for dynamically switching between different bean container implementations or adding additional
 * layers of behavior.
 * </p>
 */
public class DelegateBeanContainer implements BeanContainer, Delegate<BeanContainer> {

    /**
     * The underlying {@link BeanContainer} to which all operations are delegated.
     */
    private BeanContainer delegate;

    /**
     * Constructs a {@code DelegateBeanContainer} with the specified delegate.
     *
     * @param delegate the {@link BeanContainer} to which all operations will be delegated.
     */
    public DelegateBeanContainer(BeanContainer delegate) {
        this.delegate = delegate;
    }

    /**
     * Retrieves a bean by its name.
     *
     * @param name the name of the bean to retrieve.
     * @return the bean instance.
     */
    @Override
    public <T> T getBean(String name) {
        return delegate.getBean(name);
    }

    /**
     * Registers a bean with the given name.
     *
     * @param name the name of the bean to register.
     * @param bean the bean instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {
        delegate.registerBean(name, bean);
    }

    /**
     * Checks if a bean with the given name exists in the container.
     *
     * @param name the name of the bean.
     * @return true if the bean exists, false otherwise.
     */
    @Override
    public boolean containsBean(String name) {
        return delegate.containsBean(name);
    }

    /**
     * Sets the delegate object.
     *
     * @param delegate the delegate object to set.
     */
    @Override
    public void setDelegate(BeanContainer delegate) {
        this.delegate = delegate;
    }

    /**
     * Retrieves the current delegate object.
     *
     * @return the delegate object, or {@code null} if no delegate is set.
     */
    @Override
    public BeanContainer getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "Delegate: " + delegate.toString();
    }
}
