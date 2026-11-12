package org.jmouse.web.binding.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import org.jmouse.core.access.descriptor.AnnotatedElementDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;

/** Reads both reflection elements and the property metadata actually emitted by the mapper. */
public final class ReflectionValidationAnnotationReader implements ValidationAnnotationReader {

    @Override
    public <A extends Annotation> A findOnDestination(Object descriptor, Class<A> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType");
        if (descriptor instanceof AnnotatedElement element) {
            return element.getAnnotation(annotationType);
        }
        if (descriptor instanceof AnnotatedElementDescriptor<?, ?, ?> element) {
            return element.unwrap().getAnnotation(annotationType);
        }
        if (descriptor instanceof PropertyDescriptor<?> property) {
            var owner = property.getOwner().getType();
            var field = owner.getField(property.getName());
            if (field != null) {
                A annotation = field.unwrap().getAnnotation(annotationType);
                if (annotation != null) {
                    return annotation;
                }
            }
            for (var method : owner.getMethods().values()) {
                if (
                    (method.isGetter() || method.isSetter()) &&
                    property.getName().equals(method.getPropertyName())
                ) {
                    A annotation = method.unwrap().getAnnotation(annotationType);
                    if (annotation != null) {
                        return annotation;
                    }
                    if (method.isSetter()) {
                        annotation = method.unwrap().getParameters()[0].getAnnotation(annotationType);
                        if (annotation != null) {
                            return annotation;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public <A extends Annotation> A findOnTargetClass(Class<?> targetClass, Class<A> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType");
        return targetClass == null ? null : targetClass.getAnnotation(annotationType);
    }
}
