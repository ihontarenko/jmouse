package svit.beans.processor;

import svit.beans.BeanContext;
import svit.beans.annotation.Dependency;
import svit.reflection.Reflections;

import java.lang.reflect.Field;

public class InjectDependencyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public void postProcessAfterInitialize(Object bean, BeanContext context) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Dependency.class)) {
                Dependency dependency = field.getAnnotation(Dependency.class);
                Object value = context.getBean(field.getType(), dependency.value());
                Reflections.setFieldValue(bean, field, value);
            }
        }
    }
}
