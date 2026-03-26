package org.jmouse.beans.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.Dependency;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategies;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import static org.jmouse.core.reflection.Reflections.getFieldName;
import static org.jmouse.core.reflection.Reflections.getShortName;

public class InjectDependencyBeanPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectDependencyBeanPostProcessor.class);

    private final BeanResolutionStrategy strategy;

    public InjectDependencyBeanPostProcessor() {
        this(BeanResolutionStrategies.defaultStrategy());
    }

    public InjectDependencyBeanPostProcessor(BeanResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        Field[] fields = FieldFinder.getAnnotatedWith(bean.getClass(), Dependency.class);

        for (Field field : fields) {
            Dependency            dependency = field.getAnnotation(Dependency.class);
            BeanResolutionRequest request    = BeanResolutionRequest.forField(context, field, dependency.value());
            Object                value      = strategy.resolve(request);

            Reflections.setFieldValue(bean, field, value);

            if (value != null) {
                LOGGER.info("Dependency '{}' injected into '{}' field", getShortName(value.getClass()),
                            getFieldName(field));
            }
        }

        return bean;
    }
}