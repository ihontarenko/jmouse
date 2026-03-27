package org.jmouse.beans.processor;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.Dependency;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategies;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.beans.resolve.NullableSupport;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import static org.jmouse.core.reflection.Reflections.getFieldName;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * Injects field dependencies annotated with {@link Dependency} or {@link Inject}. 💉
 *
 * <p>Builds a {@link BeanResolutionRequest}, resolves the value, and assigns it to the field.</p>
 */
public class InjectDependencyBeanPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectDependencyBeanPostProcessor.class);

    /**
     * Resolves and injects dependencies into annotated fields before bean initialization.
     *
     * @param bean       target bean
     * @param definition bean definition
     * @param context    bean context
     * @return processed bean
     */
    @Override
    public Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        Field[]                fields   = FieldFinder.getAnnotatedWith(bean.getClass(), Dependency.class, Inject.class);
        BeanResolutionStrategy strategy = context.getBean(BeanResolutionStrategy.class);

        for (Field field : fields) {
            BeanResolutionRequest request = createBeanRequest(context, field);
            Object                value   = strategy.resolve(request);

            Reflections.setFieldValue(bean, field, value);

            if (value != null) {
                LOGGER.info("Dependency '{}' injected into '{}' field", getShortName(value.getClass()),
                            getFieldName(field));
            }
        }

        return bean;
    }

    /**
     * Creates a resolution request for the given field.
     *
     * @param context bean context
     * @param field   injection field
     * @return resolution request
     */
    private BeanResolutionRequest createBeanRequest(BeanContext context, Field field) {
        boolean      isRequired = NullableSupport.isRequired(field);
        InferredType beanType   = InferredType.forField(field);

        if (field.isAnnotationPresent(Dependency.class)) {
            String beanName = null;
            if (field.isAnnotationPresent(Qualifier.class)) {
                beanName = Reflections.getAnnotationValue(field, Qualifier.class, Qualifier::value);
            }
            return BeanResolutionRequest.forDependency(context, beanType, beanName, field, isRequired);
        }

        if (field.isAnnotationPresent(Named.class)) {
            Named named = field.getAnnotation(Named.class);
            return BeanResolutionRequest.forDependency(context, beanType, named.value(), field, isRequired);
        }

        return BeanResolutionRequest.forDependency(context, beanType, null, field, isRequired);
    }

}