package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanNotFoundException;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.conditions.ConditionEvaluator;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.context.processor.BeanPropertiesBeanPostProcessor;
import org.jmouse.context.processor.EnvironmentValueBeanPostProcessor;
import org.jmouse.context.processor.ResourceInjectionBeanPostProcessor;
import org.jmouse.core.environment.Environment;
import org.jmouse.core.environment.PropertySource;
import org.jmouse.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * 🧠 Base context with environment and resource support.
 *
 * Registers default bean post-processors for:
 * - 📦 {@code @Resource} injection<br>
 * - ⚙️ {@code @Property} binding
 *
 * Throws if required core beans are missing.
 */
public class AbstractApplicationBeanContext extends DefaultBeanContext implements ApplicationBeanContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationBeanContext.class);

    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();

    public AbstractApplicationBeanContext(BeanContext parent) {
        super(parent);
        registerRequiredComponents();
    }

    public AbstractApplicationBeanContext(Class<?>... baseClasses) {
        super(baseClasses);
        registerRequiredComponents();
    }

    /**
     * Registers a new {@link BeanDefinition} in the container.
     * <p>
     * If a bean with the same name already exists, an exception is thrown.
     * </p>
     *
     * @param definition the bean definition to register
     * @throws org.jmouse.beans.definition.DuplicateBeanDefinitionException if a bean with the same name is already registered
     */
    @Override
    public void registerDefinition(BeanDefinition definition) {
        if (conditionEvaluator.evaluate(definition, this)) {
            LOGGER.info("Registered bean definition '{}' [type={}, scope={}, primary={}, proxied={}] in context '{}'",
                        definition.getBeanName(),
                        definition.getBeanClass().getName(),
                        definition.getScope(),
                        definition.isPrimary(),
                        definition.isProxied(),
                        getContextId());
            super.registerDefinition(definition);
        } else {
            LOGGER.info("Skipped bean '{}' [type={}] – does not meet registration criteria",
                         definition.getBeanName(),
                         definition.getBeanClass().getName());
        }
    }

    /**
     * 🔧 Register default post-processors:
     * - resource injection
     * - property binding
     */
    private void registerRequiredComponents() {
        addBeanPostProcessor(new ResourceInjectionBeanPostProcessor());
        addBeanPostProcessor(new BeanPropertiesBeanPostProcessor());
        addBeanPostProcessor(new EnvironmentValueBeanPostProcessor());
    }

    /**
     * 🌱 Returns the active environment.
     *
     * @throws RuntimeException if the environment is not available
     */
    @Override
    public Environment getEnvironment() {
        try {
            return getBean(Environment.class);
        } catch (BeanNotFoundException ex) {
            throw new RuntimeException("Environment is not available in context '%s'. Likely not initialized.".formatted(getContextId()));
        }
    }

    /**
     * 📂 Resource loading utilities.
     */
    @Override
    public ResourceLoader getResourceLoader() {
        return getBean(ResourceLoader.class);
    }

    /**
     * 🔍 Get raw property value.
     */
    @Override
    public Object getRawProperty(String name) {
        return getEnvironment().getProperty(name);
    }

    /**
     * 🎯 Get property converted to type.
     */
    @Override
    public <T> T getProperty(String name, Class<T> targetType) {
        return getEnvironment().getProperty(name, targetType);
    }

    /**
     * 🗂️ Get specific property source by name.
     */
    @Override
    public PropertySource<?> getPropertySource(String name) {
        return getEnvironment().getPropertySource(name);
    }

    /**
     * ➕ Register new property source.
     */
    @Override
    public void addPropertySource(PropertySource<?> propertySource) {
        getEnvironment().addPropertySource(propertySource);
    }

    /**
     * ❓ Check if a property source is registered.
     */
    @Override
    public boolean hasPropertySource(String name) {
        return getEnvironment().hasPropertySource(name);
    }

    /**
     * ❌ Remove property source.
     */
    @Override
    public boolean removePropertySource(String name) {
        return getEnvironment().removePropertySource(name);
    }

    /**
     * 📦 All available property sources.
     */
    @Override
    public Collection<? extends PropertySource<?>> getPropertySources() {
        return getEnvironment().getPropertySources();
    }
}
