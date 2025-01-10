package svit.container;

/**
 * Interface for components that are aware of their {@link BeanContext}.
 * Provides methods to set and retrieve the {@link BeanContext}.
 */
public interface BeanContextAware {

    /**
     * Sets the {@link BeanContext} for this component.
     *
     * @param context the {@link BeanContext} to set.
     */
    void setBeanContext(BeanContext context);

    /**
     * Retrieves the {@link BeanContext} associated with this component.
     *
     * @return the current {@link BeanContext}.
     */
    BeanContext getBeanContext();
}
