package svit.container.processor;

import svit.container.BeanContext;

/**
 * A processor interface for performing operations before and after a bean is initialized.
 */
public interface BeanPostProcessor {

    /**
     * Called before the bean's initialization logic is invoked.
     * Useful for setting up properties or performing validation.
     *
     * @param bean    the bean instance to process
     * @param context the current {@link BeanContext}
     */
    default void postProcessBeforeInitialize(Object bean, BeanContext context) {}

    /**
     * Called after the bean's initialization logic completes.
     * Useful for final setup or integration tasks.
     *
     * @param bean    the bean instance to process
     * @param context the current {@link BeanContext}
     */
    default void postProcessAfterInitialize(Object bean, BeanContext context) {}
}
