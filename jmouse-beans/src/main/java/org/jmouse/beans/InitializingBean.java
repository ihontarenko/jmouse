package org.jmouse.beans;

/**
 * Callback interface for post-initialization logic. 🔄
 *
 * <p>Invoked after bean creation and dependency injection are completed.</p>
 */
public interface InitializingBean {

    /**
     * Called after bean initialization phase is finished.
     *
     * @param context bean context
     */
    void afterCompletion(BeanContext context);

}