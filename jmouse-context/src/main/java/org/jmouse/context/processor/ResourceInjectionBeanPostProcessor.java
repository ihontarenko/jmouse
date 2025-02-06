package org.jmouse.context.processor;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.processor.BeanPostProcessor;

public class ResourceInjectionBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        return BeanPostProcessor.super.postProcessAfterInitialize(bean, definition, context);
    }

}
