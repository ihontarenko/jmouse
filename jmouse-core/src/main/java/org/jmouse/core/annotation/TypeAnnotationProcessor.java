package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;

/**
 * Specialized processor for type annotations. 🏷️
 *
 * @param <A> annotation type
 */
public interface TypeAnnotationProcessor<A extends Annotation> extends AnnotationProcessor<A> {

    /**
     * Processes annotated type.
     */
    void process(Class<?> type, A annotation, AnnotationProcessingContext context);

    @Override
    default void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context) {
        if (candidate.kind() != AnnotationElementKind.TYPE) {
            return;
        }

        process((Class<?>) candidate.element(), candidate.annotation(), context);
    }
}