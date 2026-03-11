package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import static org.jmouse.core.Verify.nonNull;

/**
 * Describes a discovered annotated element. 🔎
 *
 * <p>
 * Combines the annotation instance, target element, declaring class,
 * and element kind into a single processing model.
 * </p>
 *
 * @param <A> annotation type
 */
public interface AnnotationCandidate<A extends Annotation> {

    /**
     * Returns the discovered annotation instance.
     *
     * @return annotation instance
     */
    A annotation();

    /**
     * Returns the annotated element.
     *
     * @return annotated element
     */
    AnnotatedElement element();

    /**
     * Returns the declaring or owning class.
     *
     * @return declaring class
     */
    Class<?> declaringClass();

    /**
     * Returns the candidate kind.
     *
     * @return element kind
     */
    AnnotationElementKind kind();

    /**
     * Default immutable {@link AnnotationCandidate} implementation. 🧱
     *
     * @param annotation     discovered annotation
     * @param element        annotated element
     * @param declaringClass declaring or owning class
     * @param kind           element kind
     * @param <A>            annotation type
     */
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