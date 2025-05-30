package org.jmouse.beans.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanNameKeeper;
import org.jmouse.beans.definition.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BeanPostProcessor} that populates beans implementing {@link BeanNameKeeper}
 * with their configured bean name from the {@link BeanDefinition}.
 * <p>
 * During the post-initialization phase, any bean instance that implements
 * {@code BeanNameKeeper} will have its {@code setBeanName(...)} method called
 * with the name under which it was registered in the context.
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * public class UserService implements BeanNameKeeper {
 *     private String beanName;
 *
 *     @Override
 *     public void setBeanName(String name) {
 *         this.beanName = name;
 *     }
 *
 *     // beanName can now be used for logging, metrics, etc.
 * }
 * }</pre>
 *
 * @see BeanNameKeeper
 * @see BeanPostProcessor
 */
public class BeanNameKeeperBeanPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanNameKeeperBeanPostProcessor.class);

    /**
     * After the bean has been initialized, check if it implements {@link BeanNameKeeper}.
     * If so, inject the name under which it was registered.
     *
     * @param bean       the fully initialized bean instance
     * @param definition the definition metadata for this bean, including its name
     * @param context    the {@link BeanContext} managing this bean
     * @return the same bean instance (possibly modified)
     */
    @Override
    public Object postProcessAfterInitialize(Object bean,
                                             BeanDefinition definition,
                                             BeanContext context) {
        if (bean instanceof BeanNameKeeper keeper) {
            String name = definition.getBeanName();
            LOGGER.info("Setting bean name '{}' on {}", name, bean.getClass().getSimpleName());
            keeper.setBeanName(name);
        }
        return bean;
    }
}
