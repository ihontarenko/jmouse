package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

/**
 * Processor for parameter-level annotations. 🎯
 *
 * <p>Handles {@link Parameter} elements and delegates processing with full method context.</p>
 *
 * @param <A> annotation type
 */
public interface ParameterAnnotationProcessor<A extends Annotation> extends AnnotationProcessor<A> {

    /**
     * Processes annotation on a method or constructor parameter.
     *
     * @param parameter       target parameter
     * @param annotation      annotation instance
     * @param method          declaring executable (method or constructor)
     * @param declaringClass  declaring class
     * @param context         processing context
     */
    void process(Parameter parameter, A annotation, Executable method, Class<?> declaringClass, AnnotationProcessingContext context);

    /**
     * Dispatches processing only for parameter candidates.
     */
    @Override
    default void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context) {
        if (candidate.kind() != AnnotationElementKind.PARAMETER) {
            return;
        }

        Parameter parameter = (Parameter) candidate.element();

        process(parameter, candidate.annotation(), parameter.getDeclaringExecutable(), candidate.declaringClass(), context);
    }

}