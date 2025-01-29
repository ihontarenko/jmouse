package org.jmouse.context.processor;

import svit.beans.BeanContext;
import svit.beans.definition.BeanDefinition;
import svit.beans.processor.BeanPostProcessor;

public class ResourceInjectionBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialize(Object bean, BeanDefinition definition, BeanContext context) {
        return BeanPostProcessor.super.postProcessAfterInitialize(bean, definition, context);
    }

}
