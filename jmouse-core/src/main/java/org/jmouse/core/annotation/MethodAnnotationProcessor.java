package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Specialized processor for method annotations. 🔧
 *
 * @param <A> annotation type
 */
public interface MethodAnnotationProcessor<A extends Annotation> extends AnnotationProcessor<A> {

    /**
     * Processes annotated method.
     */
    void process(Method method, A annotation, Class<?> declaringClass, AnnotationProcessingContext context);

    @Override
    default void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context) {
        if (candidate.kind() != AnnotationElementKind.METHOD) {
            return;
        }

        process((Method) candidate.element(), candidate.annotation(), candidate.declaringClass(), context);
    }
}