package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@link AnnotationProcessor} specialization for method-level annotations. 🔧
 *
 * <p>
 * Handles annotations declared on methods. The default implementation
 * filters candidates and delegates only {@link AnnotationElementKind#METHOD}
 * elements to {@link #process(Method, Annotation, Class, AnnotationProcessingContext)}.
 * </p>
 *
 * @param <A> annotation type
 */
public interface MethodAnnotationProcessor<A extends Annotation> extends AnnotationProcessor<A> {

    /**
     * Processes an annotated method.
     *
     * @param method         annotated method
     * @param annotation     annotation instance
     * @param declaringClass declaring type of the method
     * @param context        processing context
     */
    void process(Method method, A annotation, Class<?> declaringClass, AnnotationProcessingContext context);

    /**
     * Delegates processing if the candidate represents a method element.
     */
    @Override
    default void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context) {
        if (candidate.kind() != AnnotationElementKind.METHOD) {
            return;
        }

        process((Method) candidate.element(), candidate.annotation(), candidate.declaringClass(), context);
    }
}