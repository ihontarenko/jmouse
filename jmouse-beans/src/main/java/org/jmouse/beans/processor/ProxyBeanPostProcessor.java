package org.jmouse.beans.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.core.reflection.Reflections;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link BeanPostProcessor} implementation that handles proxy-related logic for beans.
 * <p>
 * This processor checks if a bean definition is marked as proxied and logs information
 * about the proxied bean during the initialization phase. It can be extended in the future
 * to apply proxying logic dynamically.
 * </p>
 *
 * @see BeanPostProcessor
 */
public class ProxyBeanPostProcessor implements BeanPostProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProxyBeanPostProcessor.class);

    /**
     * Processes a bean before its initialization.
     * <p>
     * If the bean's definition is marked as proxied, logs the bean's name and type.
     * The current implementation returns the original bean without applying any proxy logic.
     * </p>
     *
     * @param bean       the original bean instance
     * @param definition the bean's definition metadata
     * @param context    the current {@link BeanContext}
     * @return the bean instance (proxy or original)
     */
    @Override
    public Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        Object proxy = bean;

        if (definition.isProxied()) {
            ProxyFactory proxyFactory = context.getBean(ProxyFactory.class);
            Class<?>[]   ifaces       = Reflections.getClassInterfaces(bean.getClass());
            if (ifaces.length > 0) {
                LOGGER.info("Proxied bean '{}' of type '{}'",
                        definition.getBeanName(), getShortName(definition.getBeanClass()));
                proxy = proxyFactory.createProxy(bean);
            } else {
                LOGGER.error("Bean '{}' cannot be proxied with JDK Proxy. Ensure the bean implements an interface.",
                        definition.getBeanName());
            }
        }

        return proxy;
    }

}
