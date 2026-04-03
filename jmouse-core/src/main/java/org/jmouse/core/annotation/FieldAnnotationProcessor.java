package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * {@link AnnotationProcessor} specialization for field-level annotations. 🧷
 *
 * <p>
 * Handles annotations declared on fields. The default implementation
 * filters candidates and delegates only {@link AnnotationElementKind#FIELD}
 * elements to {@link #process(Field, Annotation, Class, AnnotationProcessingContext)}.
 * </p>
 *
 * @param <A> annotation type
 */
public interface FieldAnnotationProcessor<A extends Annotation> extends AnnotationProcessor<A> {

    /**
     * Processes an annotated field.
     *
     * @param field          annotated field
     * @param annotation     annotation instance
     * @param declaringClass declaring type of the field
     * @param context        processing context
     */
    void process(Field field, A annotation, Class<?> declaringClass, AnnotationProcessingContext context);

    /**
     * Delegates processing if the candidate represents a field element.
     */
    @Override
    default void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context) {
        if (candidate.kind() != AnnotationElementKind.FIELD) {
            return;
        }
        process((Field) candidate.element(), candidate.annotation(), candidate.declaringClass(), context);
    }
}