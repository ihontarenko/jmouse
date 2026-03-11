package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import static org.jmouse.core.Verify.nonNull;

/**
 * Represents a discovered annotated element. 🔎
 *
 * @param <A> annotation type
 */
public interface AnnotationCandidate<A extends Annotation> {

    /**
     * Returns discovered annotation instance.
     */
    A annotation();

    /**
     * Returns annotated element.
     */
    AnnotatedElement element();

    /**
     * Returns declaring/owning class.
     */
    Class<?> declaringClass();

    /**
     * Returns candidate kind.
     */
    AnnotationElementKind kind();

    record Default<A extends Annotation>(
            A annotation,
            AnnotatedElement element,
            Class<?> declaringClass,
            AnnotationElementKind kind
    ) implements AnnotationCandidate<A> {

        public Default {
            nonNull(annotation, "annotation");
            nonNull(element, "element");
            nonNull(declaringClass, "declaringClass");
            nonNull(kind, "kind");
        }
    }

}