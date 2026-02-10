package org.jmouse.context.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.processor.BeanPostProcessor;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.bind.Binder;

/**
 * {@link BeanPostProcessor} that binds external configuration properties into beans
 * annotated with {@link BeanProperties} before they are initialized.
 *
 * @see BeanProperties
 * @see Binder
 * @see ApplicationBeanContext#getEnvironmentBinder()
 */
public class BeanPropertiesBeanPostProcessor implements BeanPostProcessor {

    /**
     * Inspect the {@link BeanDefinition} for a {@code @BeanProperties} annotation. If present,
     * force the bean’s scope to {@link BeanScope#SINGLETON}, then bind environment properties
     * into the bean using the application’s {@link Binder}.
     *
     * @param bean       the bean instance being processed; must not be {@code null}
     * @param definition the bean’s metadata, including any annotations; must not be {@code null}
     * @param context    the current {@link BeanContext} managing the bean; must be an instance of {@link ApplicationBeanContext}
     * @return the same bean instance (unmodified or with bound properties)
     * @throws IllegalArgumentException if any argument is {@code null}
     * @throws IllegalStateException    if the context is not an {@link ApplicationBeanContext}
     */
    @Override
    public Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        BeanProperties annotation = definition.getAnnotation(BeanProperties.class);

        if (annotation != null) {
            if (!(context instanceof ApplicationBeanContext beanContext)) {
                throw new IllegalStateException("BeanProperties can only be processed in an ApplicationBeanContext");
            }

            // Force bean to be singleton so that bound properties persist throughout application
            definition.setScope(BeanScope.SINGLETON);

            // Bind the environment properties at the specified path into this bean
            TypedValue<?> bindable = TypedValue.ofInstance(bean);
            String        path     = annotation.value();

            if (path == null || path.isBlank()) {
                throw new IllegalStateException("@BeanProperties path must not be null or blank");
            }

            Binder binder = beanContext.getEnvironmentBinder();

            if (binder == null) {
                throw new IllegalStateException("No environment Binder available in ApplicationBeanContext");
            }

            binder.bind(path, bindable);
        }

        return bean;
    }
}
