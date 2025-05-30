package org.jmouse.context.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.processor.BeanPostProcessor;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.Bindable;
import org.jmouse.core.bind.Binder;

public class BeanPropertiesBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        BeanProperties annotation = definition.getAnnotation(BeanProperties.class);
        Binder         binder     = null;

        if (annotation == null) {
            return bean;
        }

        if (context instanceof ApplicationBeanContext beanContext) {
            Bindable<?> bindable = Bindable.ofInstance(bean);
            String      path     = annotation.value();

            binder = beanContext.getEnvironmentBinder();
            binder.bind(path, bindable);
        }

        return bean;
    }

}
