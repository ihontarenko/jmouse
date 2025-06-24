package org.jmouse.beans.conditions;

import org.jmouse.beans.definition.BeanDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public interface ConditionalMetadata {

    BeanDefinition getBeanDefinition();

    Annotation getAnnotation();

    default <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        Annotation annotation = getAnnotation();

        if (annotationType.isInstance(annotation)) {
            return annotationType.cast(annotation);
        }

        throw new ClassCastException(annotation.getClass().getName() + " is not a " + annotationType.getName());
    }

    AnnotatedElement getAnnotatedElement();

}
