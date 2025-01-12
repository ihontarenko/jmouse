package svit.beans.processor;

import svit.beans.BeanContext;
import svit.beans.support.EnvironmentValue;
import svit.reflection.Reflections;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PropertyValueBeanProcessor implements BeanPostProcessor {

    private final Map<Object, Object> globals;

    public PropertyValueBeanProcessor(Map<?, ?>... maps) {
        Map<Object, Object> globals = new HashMap<>();

        for (Map<?, ?> map : maps) {
            globals.putAll(map);
        }

        this.globals = globals;
    }

    @Override
    public void postProcessAfterInitialize(Object bean, BeanContext context) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(EnvironmentValue.class)) {
                if (String.class.isAssignableFrom(field.getType())) {
                    EnvironmentValue variable = field.getAnnotation(EnvironmentValue.class);
                    Reflections.setFieldValue(bean, field, globals.get(variable.value()));
                } else {
                    throw new BeanProcessorException(
                            "Field '%s' annotated as '%s' must be '%s' type"
                                    .formatted(field, EnvironmentValue.class, String.class.getName()));
                }
            }
        }
    }
}
