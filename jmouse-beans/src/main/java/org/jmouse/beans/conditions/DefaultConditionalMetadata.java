package org.jmouse.beans.conditions;

import org.jmouse.beans.definition.BeanDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public class DefaultConditionalMetadata implements ConditionalMetadata {

    private final Annotation         annotation;
    private final BeanDefinition beanDefinition;

    public DefaultConditionalMetadata(Annotation annotation, BeanDefinition beanDefinition) {
        this.annotation = annotation;
        this.beanDefinition = beanDefinition;
    }

    @Override
    public BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }

    @Override
    public Annotation getAnnotation() {
        return annotation;
    }

    @Override
    public AnnotatedElement getAnnotatedElement() {
        return beanDefinition.getAnnotatedElement();
    }
}
