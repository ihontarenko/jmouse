package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record AnnotationData(Annotation annotation, AnnotatedElement annotatedElement, AnnotationData parent, Set<AnnotationData> metas,
                             int depth) {

    public AnnotationData(Annotation annotation, AnnotatedElement annotatedElement, AnnotationData parent, int depth) {
        this(annotation, annotatedElement, parent, new LinkedHashSet<>(), depth);
    }

    public Set<AnnotationData> metas() {
        return metas;
    }

    public Class<? extends Annotation> annotationType() {
        return annotation.annotationType();
    }

    public Optional<AnnotationData> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AnnotationData that)) {
            return false;
        }

        return Objects.equals(annotation, that.annotation)
                && Objects.equals(annotatedElement, that.annotatedElement)
                && Objects.equals(depth, that.depth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotation, annotatedElement, depth);
    }

    @Override
    public String toString() {
        return "Annotation @%s on element: %s%s, depth=%d, metas=%d"
                .formatted(annotationType().getSimpleName(), annotatedElement,
                           parent != null ? ", meta of: @" + parent.annotationType().getSimpleName() : "",
                           depth, metas.size());
    }
}
