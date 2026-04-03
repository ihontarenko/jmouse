package org.jmouse.context.definition;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.strategy.AbstractBeanDefinitionCreationStrategy;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.Priority;

import java.util.Set;

/**
 * Creates {@link RecordPropertiesDefinition} for record classes annotated with {@link BeanProperties}. 🧩
 */
@Priority(Integer.MIN_VALUE + 500)
public class RecordPropertiesDefinitionCreationStrategy extends AbstractBeanDefinitionCreationStrategy<Class<?>> {

    /**
     * Checks whether the given object is a record annotated with {@link BeanProperties}.
     *
     * @param object candidate object
     * @return {@code true} if supported
     */
    @Override
    public boolean supports(Object object) {
        return (object instanceof Class<?> type)
                && type.isAnnotationPresent(BeanProperties.class)
                && type.isRecord();
    }

    /**
     * Creates {@link RecordPropertiesDefinition} for the given record type.
     *
     * @param name    bean name
     * @param type    record type
     * @param context bean context
     * @return bean definition
     */
    @Override
    public BeanDefinition create(String name, Class<?> type, BeanContext context) {
        RecordPropertiesDefinition definition = new RecordPropertiesDefinition(name, type);
        updateBeanDefinition(definition, type);
        definition.setAnnotations(Set.of(type.getAnnotations()));
        return definition;
    }

}