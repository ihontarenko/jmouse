package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 📎 Represents a single annotation with metadata context.
 *
 * @param annotation       the actual annotation
 * @param annotatedElement the target element
 * @param parent           parent meta-annotation (if any)
 * @param metas            direct meta-annotations
 * @param depth            level in hierarchy
 */
public record AnnotationData(
        Annotation annotation,
        AnnotatedElement annotatedElement,
        AnnotationData parent,
        Set<AnnotationData> metas,
        int depth
) {

    /**
     * 📌 Constructs a data record with an empty set of meta-annotations.
     */
    public AnnotationData(Annotation annotation, AnnotatedElement annotatedElement, AnnotationData parent, int depth) {
        this(annotation, annotatedElement, parent, new LinkedHashSet<>(), depth);
    }

    /**
     * 🔁 Meta-annotations directly applied on this annotation.
     */
    public Set<AnnotationData> metas() {
        return metas;
    }

    /**
     * 🧬 Annotation type.
     */
    public Class<? extends Annotation> annotationType() {
        return annotation.annotationType();
    }

    /**
     * 🔗 Parent annotation if this is a meta-annotation.
     */
    public Optional<AnnotationData> getMetaOf() {
        return Optional.ofNullable(parent);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;

        if (!(object instanceof AnnotationData that))
            return false;

        return Objects.equals(annotation, that.annotation)
                && Objects.equals(annotatedElement, that.annotatedElement)
                && depth == that.depth;
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
