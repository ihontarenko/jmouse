package org.jmouse.beans.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.Dependency;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Field;

import static org.jmouse.core.reflection.Reflections.getFieldName;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link BeanPostProcessor} implementation for injecting dependencies into fields
 * annotated with {@link Dependency}.
 * <p>
 * This processor scans the bean's class for fields annotated with {@link Dependency},
 * retrieves the corresponding bean instance from the {@link BeanContext}, and injects
 * it into the annotated field.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @Dependency("userService")
 * private UserService userService;
 * }</pre>
 */
public class InjectDependencyBeanPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectDependencyBeanPostProcessor.class);

    /**
     * Processes the given bean instance before initialization by injecting dependencies
     * into fields annotated with {@link Dependency}.
     *
     * @param bean       the bean instance to process.
     * @param definition the {@link BeanDefinition} associated with the bean.
     * @param context    the {@link BeanContext} for retrieving dependencies.
     * @return the processed bean instance with dependencies injected.
     */
    @Override
    public Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        Field[] fields = FieldFinder.getAnnotatedWith(bean.getClass(), Dependency.class);

        for (Field field : fields) {
            Dependency dependency = field.getAnnotation(Dependency.class);
            Object value = context.getBean(field.getType(), dependency.value());
            Reflections.setFieldValue(bean, field, value);
            LOGGER.info("Dependency '{}' injected into '{}' field", getShortName(value.getClass()), getFieldName(field));
        }

        return bean;
    }
}
