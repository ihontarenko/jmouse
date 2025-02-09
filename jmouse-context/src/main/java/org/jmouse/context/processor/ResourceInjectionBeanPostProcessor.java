package org.jmouse.context.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.processor.BeanPostProcessor;
import org.jmouse.context.Resource;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.FieldMatchers;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.jmouse.core.reflection.FieldMatchers.isAnnotatedWith;

public class ResourceInjectionBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        FieldFinder       finder = new FieldFinder();
        Collection<Field> fields = finder.find(definition.getBeanClass(), isAnnotatedWith(Resource.class));

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                field.set(bean, null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return bean;
    }

}
