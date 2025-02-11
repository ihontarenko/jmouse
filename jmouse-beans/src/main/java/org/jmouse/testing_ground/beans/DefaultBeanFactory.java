package org.jmouse.testing_ground.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.testing_ground.beans.instantiation.BeanInstantiationFactory;
import org.jmouse.testing_ground.beans.instantiation.BeanInstantiationStrategy;
import org.jmouse.testing_ground.beans.definition.BeanDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * The default implementation of {@link BeanFactory} and {@link BeanInstantiationFactory}.
 * Provides logic for instantiating beans using registered {@link BeanInstantiationStrategy} instances.
 *
 * <p>Key responsibilities:
 * <ul>
 *     <li>Maintaining a list of {@link BeanInstantiationStrategy} to create beans.</li>
 *     <li>Delegating bean instantiation to the appropriate strategy.</li>
 *     <li>Integrating with a {@link BeanContext} for dependency resolution.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * DefaultBeanFactory factory = new DefaultBeanFactory();
 * factory.addStrategy(new SimpleBeanInstantiationStrategy());
 * // ...
 * BeanDefinition definition = ...;
 * Object bean = factory.createBean(definition);
 * }</pre>
 */
public class DefaultBeanFactory implements BeanFactory, BeanInstantiationFactory, BeanContextAware {

    /**
     * Logger for logging events in {@code DefaultBeanFactory}.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBeanFactory.class);

    /**
     * The list of {@link BeanInstantiationStrategy} instances used to create bean objects.
     */
    private final List<BeanInstantiationStrategy> strategies = new ArrayList<>();

    /**
     * The current {@link BeanContext}, used for resolving dependencies and other context-related data.
     */
    private BeanContext context;

    /**
     * Creates a new bean instance based on the given {@link BeanDefinition}.
     * <p>
     * This method delegates to {@link #createInstance(BeanDefinition, BeanContext)}.
     * If the instance is {@code null}, a {@link BeanInstantiationException} is thrown.
     * The created instance is then assigned to the bean definition.
     *
     * @param definition the bean definition describing how the bean should be created
     * @param <T>        the expected type of the bean
     * @return the newly created bean instance
     * @throws BeanInstantiationException if bean instantiation fails or yields a {@code null} instance
     */
    @Override
    public <T> T createBean(BeanDefinition definition) {
        T instance = (T) createInstance(definition, getBeanContext());

        if (instance == null) {
            throw new BeanInstantiationException(
                    "Failed to instantiate bean '%s' using definition of type '%s'."
                            .formatted(definition.getBeanName(), getShortName(definition.getClass())));
        }

        // assign bean instance for it definition
        definition.setBeanInstance(instance);

        return instance;
    }

    /**
     * Creates a bean instance by iterating over registered {@link BeanInstantiationStrategy} objects.
     * When a strategy supports the given definition, it is used to create the bean.
     * The first matching strategy sets the creation strategy in the definition and logs the creation.
     *
     * @param definition the bean definition to instantiate
     * @param context    the current {@link BeanContext}
     * @return the created bean instance, or {@code null} if no strategy supports the definition
     */
    @Override
    public Object createInstance(BeanDefinition definition, BeanContext context) {
        Object instance = null;

        for (BeanInstantiationStrategy strategy : strategies) {
            if (strategy.supports(definition)) {
                definition.setBeanCreationStrategy(strategy);
                instance = strategy.create(definition, context);
                LOGGER.info("Bean instance (scope='{}', name='{}') created via: '{}' strategy",
                            definition.getScope(), definition.getBeanName(), getShortName(strategy.getClass()));
            }
        }

        return instance;
    }

    /**
     * Registers a new {@link BeanInstantiationStrategy}.
     *
     * @param strategy the strategy to add
     */
    @Override
    public void addStrategy(BeanInstantiationStrategy strategy) {
        LOGGER.info("Register new strategy '{}'", getShortName(strategy.getClass()));
        strategies.add(strategy);
    }

    /**
     * Retrieves the current {@link BeanContext}.
     *
     * @return the associated bean context
     */
    @Override
    public BeanContext getBeanContext() {
        return context;
    }

    /**
     * Sets the current {@link BeanContext} for this factory.
     *
     * @param context the bean context to associate
     */
    @Override
    public void setBeanContext(BeanContext context) {
        this.context = context;
    }


}