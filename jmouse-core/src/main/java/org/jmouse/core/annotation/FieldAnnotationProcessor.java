package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Specialized processor for field annotations. 🧷
 *
 * @param <A> annotation type
 */
public interface FieldAnnotationProcessor<A extends Annotation> extends AnnotationProcessor<A> {

    /**
     * Processes annotated field.
     */
    void process(Field field, A annotation, Class<?> declaringClass, AnnotationProcessingContext context);

    @Override
    default void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context) {
        if (candidate.kind() != AnnotationElementKind.FIELD) {
            return;
        }

        process((Field) candidate.element(), candidate.annotation(), candidate.declaringClass(), context);
    }
}