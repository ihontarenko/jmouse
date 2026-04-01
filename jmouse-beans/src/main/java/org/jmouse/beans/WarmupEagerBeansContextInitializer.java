package org.jmouse.beans;

import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes eager beans during context startup. 🔥
 *
 * <p>Iterates over all {@link BeanDefinition definitions} and triggers
 * instantiation for those marked as eager.</p>
 */
@Priority(-500)
public class WarmupEagerBeansContextInitializer implements BeanContextInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarmupEagerBeansContextInitializer.class);

    /**
     * Triggers eager initialization for beans marked with {@code eager=true}.
     *
     * @param context bean context
     */
    @Override
    public void initialize(BeanContext context) {
        for (BeanDefinition definition : context.getDefinitions()) {
            if (definition.isEager()) {
                String beanName     = definition.getBeanName();
                Object beanInstance = definition.getBeanInstance();

                if (beanInstance != null) {
                    LOGGER.info("⚠️ Bean [{}] already initialized, skipping warmup 🔁", beanName);
                }

                beanInstance = context.getBean(beanName);

                if (beanInstance != null) {
                    LOGGER.info("🔥 Bean [{}] eagerly initialized successfully 🚀", beanName);
                } else {
                    LOGGER.warn("❌ Bean [{}] eager initialization failed", beanName);
                }
            }
        }
    }

}