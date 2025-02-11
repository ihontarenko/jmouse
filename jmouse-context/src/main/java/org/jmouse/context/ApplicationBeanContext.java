package org.jmouse.context;

import org.jmouse.testing_ground.beans.BeanContext;
import org.jmouse.testing_ground.beans.BeanFactory;
import org.jmouse.core.env.Environment;
import org.jmouse.core.env.PropertyResolver;
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

    /**
     * Retrieves the current {@link Environment} associated with this context.
     *
     * @return the {@link Environment} instance
     */
    Environment getEnvironment();

    /**
     * Retrieves the {@link ResourceLoader} for accessing resources.
     *
     * @return the {@link ResourceLoader} instance
     */
    ResourceLoader getResourceLoader();

}
