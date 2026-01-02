package org.jmouse.context.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.processor.BeanPostProcessor;
import org.jmouse.beans.processor.BeanProcessorException;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.context.EnvironmentValue;
import org.jmouse.core.bind.BeanPropertyNotFound;
import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.Binder;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.accessor.JavaBeanAccessor;
import org.jmouse.core.environment.Environment;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.jmouse.core.reflection.FieldMatchers.isAnnotatedWith;

/**
 * 🌱 Injects environment properties into bean fields marked with {@link EnvironmentValue}.
 * <p>
 * Uses {@link Binder} to resolve values from the environment and set them via JavaBean accessor.
 * </p>
 *
 * Example:
 * <pre>{@code
 * public class ApiCallService {
 *     @EnvironmentValue("app.timeout")
 *     private int timeout;
 * }
 * }</pre>
 *
 * 🔧 Requires setter or accessible field. Throws {@link BeanProcessorException} if property can't be set.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class EnvironmentValueBeanPostProcessor implements BeanPostProcessor {

    private static final Logger      LOGGER       = LoggerFactory.getLogger(EnvironmentValueBeanPostProcessor.class);
    private static final FieldFinder FIELD_FINDER = new FieldFinder();

    @Override
    public Object postProcessAfterInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        Collection<Field> fields = FIELD_FINDER.find(definition.getBeanClass(), isAnnotatedWith(EnvironmentValue.class));

        if (!(context instanceof ApplicationBeanContext applicationContext) || fields.isEmpty()) {
            return bean;
        }

        LOGGER.info("🔍 Processing @EnvironmentValue fields for bean '{}'", definition.getBeanName());

        Environment      environment = applicationContext.getEnvironment();
        Binder           binder      = new Binder(ObjectAccessor.wrapObject(environment));
        JavaBeanAccessor accessor    = new JavaBeanAccessor(bean);

        for (Field field : fields) {
            EnvironmentValue annotation  = MergedAnnotation.wrapWithSynthetic(field)
                    .getNativeAnnotation(EnvironmentValue.class);
            String           propertyKey = annotation.value();
            Object           value       = Bind.with(binder).get(propertyKey, field.getType());

            try {
                accessor.set(field.getName(), value);
                LOGGER.info("✅ Injected environment value '{}' into field '{}.{}' (type: {})",
                            propertyKey, bean.getClass().getSimpleName(), field.getName(), field.getType().getSimpleName());
            } catch (BeanPropertyNotFound notFound) {
                throw new BeanProcessorException(
                        "❌ Cannot inject env property '%s': no writable field '%s' in bean '%s'"
                                .formatted(propertyKey, field.getName(), definition.getBeanName()),
                        notFound);
            }
        }

        return bean;
    }
}
