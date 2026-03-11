package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;

/**
 * {@link AnnotationProcessor} specialization for type-level annotations. 🏷️
 *
 * <p>
 * Processes annotations declared on classes, interfaces, records, or enums.
 * The default implementation filters candidates and delegates only
 * {@link AnnotationElementKind#TYPE} elements to {@link #process(Class, Annotation, AnnotationProcessingContext)}.
 * </p>
 *
 * @param <A> annotation type
 */
public interface TypeAnnotationProcessor<A extends Annotation> extends AnnotationProcessor<A> {

    /**
     * Processes an annotated type.
     *
     * @param type       annotated class
     * @param annotation annotation instance
     * @param context    processing context
     */
    void process(Class<?> type, A annotation, AnnotationProcessingContext context);

    /**
     * Delegates processing if the candidate represents a type element.
     */
    @Override
    default void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context) {
        if (candidate.kind() != AnnotationElementKind.TYPE) {
            return;
        }

        process((Class<?>) candidate.element(), candidate.annotation(), context);
    }
}