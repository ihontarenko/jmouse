package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanFactory;
import org.jmouse.beans.BeanNotFoundException;
import org.jmouse.beans.BeanScope;
import org.jmouse.core.bind.Binder;
import org.jmouse.core.environment.Environment;
import org.jmouse.core.environment.PropertyResolver;
import org.jmouse.core.io.ResourceLoader;

/**
 * A specialized {@link BeanContext} that combines functionality from {@link BeanFactory},
 * {@link PropertyResolver}, and environment/resource management.
 * <p>
 * This interface is designed for use in application-level contexts, providing access
 * to beans, environment properties, and resources.
 * </p>
 *
 * @see BeanContext
 * @see BeanFactory
 * @see PropertyResolver
 * @see Environment
 */
public interface ApplicationBeanContext extends BeanContext, BeanFactory, PropertyResolver {

    String ENVIRONMENT_BINDER_BEAN_NAME = "environmentBinder";

    /**
     * Retrieves the current {@link Environment} associated with this context.
     *
     * @return the {@link Environment} instance
     */
    Environment getEnvironment();

    /**
     * Retrieves the current {@link Environment} associated with this context.
     *
     * @return the {@link Environment} instance
     */
    default Binder getEnvironmentBinder() {
        try {
            return getBean(Binder.class, ENVIRONMENT_BINDER_BEAN_NAME);
        } catch (BeanNotFoundException e) {
            Binder binder = Binder.forObject(getEnvironment());
            registerBean(ApplicationBeanContext.ENVIRONMENT_BINDER_BEAN_NAME, binder, BeanScope.SINGLETON);
            return binder;
        }
    }

    /**
     * Retrieves the {@link ResourceLoader} for accessing resources.
     *
     * @return the {@link ResourceLoader} instance
     */
    ResourceLoader getResourceLoader();

}
