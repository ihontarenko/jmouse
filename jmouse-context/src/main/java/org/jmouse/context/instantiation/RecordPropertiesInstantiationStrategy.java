package org.jmouse.context.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.instantiation.AbstractBeanInstantiationStrategy;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.context.BeanContextInstantiationType;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.binding.BindResult;
import org.jmouse.core.binding.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Instantiation strategy for record beans backed by {@link BeanProperties}. 🧩
 *
 * <p>Reads configuration values from the application environment and binds them to a record type.</p>
 */
@Priority(Integer.MIN_VALUE + 500)
public class RecordPropertiesInstantiationStrategy extends AbstractBeanInstantiationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordPropertiesInstantiationStrategy.class);

    /**
     * Creates a record bean by binding configuration properties.
     *
     * @param definition bean definition
     * @param context    bean context
     * @return bound record instance or {@code null} if binding did not produce a value
     */
    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        AtomicReference<Object> properties = new AtomicReference<>();

        if (context instanceof ApplicationBeanContext applicationContext) {
            BeanProperties annotation = definition.getAnnotation(BeanProperties.class);
            Binder         binder     = applicationContext.getEnvironmentBinder();
            TypedValue<?>  typedValue = TypedValue.of(definition.getBeanClass());
            BindResult<?>  result     = binder.bind(annotation.value(), typedValue);

            LOGGER.debug("@BeanProperties '{}' for record was created!", typedValue.getType());

            result.ifPresent(properties::set);
        }

        return properties.get();
    }

    /**
     * Checks whether this strategy supports the given bean definition.
     *
     * @param definition bean definition
     * @return {@code true} if instantiation type is record bean properties
     */
    @Override
    public boolean supports(BeanDefinition definition) {
        return definition.getInstantiationType().isTheSame(BeanContextInstantiationType.RECORD_BEAN_PROPERTIES);
    }

}