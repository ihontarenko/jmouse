package org.jmouse.context.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.processor.BeanPostProcessor;
import org.jmouse.context.Resource;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.io.ResourceLoader;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.reflection.FieldMatchers.isAnnotatedWith;

public class ResourceInjectionBeanPostProcessor implements BeanPostProcessor {

    private final FieldFinder finder = new FieldFinder();

    @Override
    public Object postProcessAfterInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        Collection<Field> fields = finder.find(definition.getBeanClass(), isAnnotatedWith(Resource.class));
        ResourceLoader    loader = context.getBean(ResourceLoader.class);

        for (Field field : fields) {
            try {
                String resource = Reflections.getAnnotationValue(field, Resource.class, Resource::value);
                field.setAccessible(true);
                field.set(bean, convertIfNeeded(loader.getResource(resource), field, context));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return bean;
    }

    private Object convertIfNeeded(Object value, Field field, BeanContext context) {
        Class<?> targetType = field.getType();
        Class<?> sourceType = nonNull(value, "value").getClass();

        if (targetType.isAssignableFrom(sourceType)) {
            return value;
        }

        Conversion conversion = context.getBean(Conversion.class);

        return conversion.convertIfNeeded(value, targetType);
    }

}
