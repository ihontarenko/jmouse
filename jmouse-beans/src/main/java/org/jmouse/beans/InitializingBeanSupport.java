package org.jmouse.beans;

/**
 * 🔧 Extended {@link InitializingBean} with typed context support.
 * <p>
 * Provides a template initialization workflow:
 * <ul>
 *   <li>{@link #afterCompletion(BeanContext)} – casts and delegates to {@link #initialize(BeanContext)}</li>
 *   <li>{@link #initialize(BeanContext)} – calls {@link #doInitialize(BeanContext)}</li>
 *   <li>{@link #doInitialize(BeanContext)} – subclass hook for custom logic</li>
 * </ul>
 *
 * @param <C> the specific {@link BeanContext} subtype
 */
public interface InitializingBeanSupport<C extends BeanContext> extends InitializingBean {

    /**
     * Called when bean creation is complete.
     * Casts the generic {@link BeanContext} to the expected type and delegates to {@link #initialize(BeanContext)}.
     *
     * @param context the created bean context
     */
    @Override
    @SuppressWarnings("unchecked")
    default void afterCompletion(BeanContext context) {
        initialize((C) context);
    }

    /**
     * Initialize this bean with the given context.
     * <p>Default implementation calls {@link #doInitialize(BeanContext)}.
     *
     * @param context the typed bean context
     */
    default void initialize(C context) {
        doInitialize(context);
    }

    /**
     * Hook for custom initialization logic after the context is ready.
     * Default implementation does nothing.
     *
     * @param context the typed bean context
     */
    default void doInitialize(C context) {
    }
}
