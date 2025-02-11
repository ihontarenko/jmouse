package org.jmouse.beans.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.definition.strategy.BeanDefinitionCreationStrategy;
import org.jmouse.core.matcher.Matcher;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getShortName;
import static org.jmouse.core.reflection.TypeMatchers.extendsGenericSuperclass;
import static org.jmouse.core.reflection.TypeMatchers.implementsGenericInterface;

/**
 * A simple implementation of the {@link BeanDefinitionFactory} that uses a list of strategies
 * to create bean definitions.
 * <p>
 * This factory supports the registration of custom {@link BeanDefinitionCreationStrategy} instances,
 * which are used to process different types of objects and generate bean definitions.
 */
public class SimpleBeanDefinitionFactory implements BeanDefinitionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBeanDefinitionFactory.class);

    /**
     * List of registered {@link BeanDefinitionCreationStrategy} instances.
     */
    private final List<BeanDefinitionCreationStrategy<?>> strategies = new ArrayList<>();

    /**
     * Registers a new {@link BeanDefinitionCreationStrategy}.
     *
     * @param strategy the strategy to register.
     */
    @Override
    public void addStrategy(BeanDefinitionCreationStrategy<?> strategy) {
        LOGGER.info("Registering new strategy '{}'", getShortName(strategy.getClass()));
        strategies.add(strategy);
    }

    /**
     * Creates a {@link BeanDefinition} for the given object using the registered strategies.
     * <p>
     * If a preferred name is provided, it is passed to the strategy for naming the bean definition.
     * The factory iterates over its strategies to find a suitable one that supports the given object.
     * If no suitable strategy is found, a {@link BeanDefinitionException} is thrown.
     * <p>
     * If the created bean definition does not specify a scope, it defaults to {@link BeanScope#SINGLETON}.
     *
     * @param preferredName the preferred name for the bean, or {@code null} to use the default naming strategy.
     * @param object        the object for which to create the bean definition.
     * @param context       the {@link BeanContext} used during creation.
     * @return the created {@link BeanDefinition}.
     * @throws BeanDefinitionException if no suitable strategy is found.
     */
    @Override
    public BeanDefinition createDefinition(String preferredName, Object object, BeanContext context) {
        BeanDefinition definition = null;

        if (object == null) {
            LOGGER.error("Bean definition creation skipped: input object is NULL.");
            return definition;
        }

        Class<?> objectClass = object.getClass();

        for (BeanDefinitionCreationStrategy<?> strategy : strategies) {
            Class<?>          strategyClass = strategy.getClass();
            Matcher<Class<?>> matcher       = extendsGenericSuperclass(objectClass)
                    .or(implementsGenericInterface(BeanDefinitionCreationStrategy.class, objectClass));

            if (strategy.supports(object)) {
                if (!matcher.matches(strategyClass)) {
                    LOGGER.error("Applicable strategy '{}' found, but additional validation failed for type '{}'.",
                            getShortName(strategyClass), getShortName(objectClass));
                    continue;
                }

                BeanDefinitionCreationStrategy<Object> typedStrategy = ((BeanDefinitionCreationStrategy<Object>)strategy);

                definition = preferredName == null
                        ? typedStrategy.create(object, context) : typedStrategy.create(preferredName, object, context);

                LOGGER.info("Definition bean '{}' created. With: '{}'.",definition.getBeanName(),
                            getShortName(strategy.getClass()));
            }
        }

        if (definition == null) {
            throw new BeanDefinitionException(
                    "No strategy found to create a bean definition for the object type '%s' and preferred name '%s'."
                            .formatted(objectClass, preferredName));
        }

        if (definition.getScope() == null) {
            definition.setScope(BeanScope.SINGLETON);
        }

        return definition;
    }

}
