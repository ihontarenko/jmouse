package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

public record AnnotationData(Annotation annotation, AnnotatedElement annotatedElement, AnnotationData parent,
                             int depth) {

    public Class<? extends Annotation> annotationType() {
        return annotation.annotationType();
    }

    public Optional<AnnotationData> getParent() {
        return Optional.ofNullable(parent);
    }

}
