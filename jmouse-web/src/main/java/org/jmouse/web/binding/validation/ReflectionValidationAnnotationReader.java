package org.jmouse.web.binding.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public final class ReflectionValidationAnnotationReader implements ValidationAnnotationReader {

    @Override
    public <A extends Annotation> A findOnDestination(Object destinationDescriptor, Class<A> annotationType) {
        if (destinationDescriptor instanceof AnnotatedElement element) {
            return element.getAnnotation(annotationType);
        }
        return null;
    }

    @Override
    public <A extends Annotation> A findOnTargetClass(Class<?> targetClass, Class<A> annotationType) {
        if (targetClass == null) {
            return null;
        }
        return targetClass.getAnnotation(annotationType);
    }
}
