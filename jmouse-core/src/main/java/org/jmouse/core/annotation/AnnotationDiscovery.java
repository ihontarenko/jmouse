package org.jmouse.core.annotation;

import org.jmouse.core.Verify;
import org.jmouse.core.reflection.ClassFinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers annotated elements in classpath. 🔍
 */
public interface AnnotationDiscovery {

    /**
     * Finds all candidates for the given annotation type.
     *
     * @param annotationType annotation type
     * @param baseClasses    scan roots
     * @param <A>            annotation type
     * @return discovered candidates
     */
    <A extends Annotation> List<AnnotationCandidate<A>> findCandidates(
            Class<A> annotationType,
            Class<?>... baseClasses
    );

    class Default implements AnnotationDiscovery {

        @Override
        public <A extends Annotation> List<AnnotationCandidate<A>> findCandidates(
                Class<A> annotationType,
                Class<?>... baseClasses
        ) {
            Verify.nonNull(annotationType, "annotationType");

            List<AnnotationCandidate<A>> candidates = new ArrayList<>();

            for (Class<?> type : ClassFinder.findAll(clazz -> true, baseClasses)) {
                collectTypeCandidate(annotationType, type, candidates);
                collectMethodCandidates(annotationType, type, candidates);
                collectFieldCandidates(annotationType, type, candidates);
            }

            return List.copyOf(candidates);
        }

        private <A extends Annotation> void collectTypeCandidate(
                Class<A> annotationType,
                Class<?> type,
                List<AnnotationCandidate<A>> candidates
        ) {
            A annotation = type.getAnnotation(annotationType);

            if (annotation != null) {
                candidates.add(new AnnotationCandidate.Default<>(
                        annotation,
                        type,
                        type,
                        AnnotationElementKind.TYPE
                ));
            }
        }

        private <A extends Annotation> void collectMethodCandidates(
                Class<A> annotationType,
                Class<?> type,
                List<AnnotationCandidate<A>> candidates
        ) {
            for (Method method : type.getDeclaredMethods()) {
                A annotation = method.getAnnotation(annotationType);

                if (annotation != null) {
                    candidates.add(new AnnotationCandidate.Default<>(
                            annotation,
                            method,
                            type,
                            AnnotationElementKind.METHOD
                    ));
                }
            }
        }

        private <A extends Annotation> void collectFieldCandidates(
                Class<A> annotationType,
                Class<?> type,
                List<AnnotationCandidate<A>> candidates
        ) {
            for (Field field : type.getDeclaredFields()) {
                A annotation = field.getAnnotation(annotationType);

                if (annotation != null) {
                    candidates.add(new AnnotationCandidate.Default<>(
                            annotation,
                            field,
                            type,
                            AnnotationElementKind.FIELD
                    ));
                }
            }
        }
    }

}