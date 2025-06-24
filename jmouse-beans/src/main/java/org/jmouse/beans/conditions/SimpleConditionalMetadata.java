package org.jmouse.beans.conditions;

import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.annotation.MergedAnnotation;

import java.lang.reflect.AnnotatedElement;

/**
 * 📦 Simple conditional metadata for a single merged annotation.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class SimpleConditionalMetadata implements ConditionalMetadata {

    private final MergedAnnotation annotation;
    private final BeanDefinition   beanDefinition;

    public SimpleConditionalMetadata(MergedAnnotation annotation, BeanDefinition beanDefinition) {
        this.annotation = annotation;
        this.beanDefinition = beanDefinition;
    }

    /**
     * 🎯 The target bean definition.
     */
    @Override
    public BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }

    /**
     * 🧷 The merged annotation context.
     */
    @Override
    public MergedAnnotation getMergedAnnotation() {
        return annotation;
    }

    /**
     * 📌 Annotated element (usually class or method).
     */
    @Override
    public AnnotatedElement getAnnotatedElement() {
        return beanDefinition.getAnnotatedElement();
    }
}
