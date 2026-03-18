package org.jmouse.core.annotation;

import org.jmouse.core.Verify;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassFinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers annotation candidates in the classpath. 🔍
 *
 * <p>
 * Scans provided base classes and inspects discovered types, methods,
 * and fields for the requested annotation.
 * </p>
 */
public interface AnnotationDiscovery {

    /**
     * Finds candidates for the given annotation type.
     *
     * @param annotationType annotation type to search for
     * @param baseClasses    scan roots
     * @param <A>            annotation type
     *
     * @return immutable list of discovered candidates
     */
    <A extends Annotation> List<AnnotationCandidate<A>> findCandidates(
            Class<A> annotationType,
            Class<?>... baseClasses
    );

    /**
     * Returns default discovery implementation.
     *
     * @return default discovery
     */
    static AnnotationDiscovery defaults() {
        return new Default();
    }

    /**
     * Default {@link AnnotationDiscovery} implementation. 🧱
     */
    class Default implements AnnotationDiscovery {

        /**
         * Scans classes reachable from the given roots and collects
         * type, method, and field annotation candidates.
         */
        @Override
        public <A extends Annotation> List<AnnotationCandidate<A>> findCandidates(
                Class<A> annotationType,
                Class<?>... baseClasses
        ) {
            Verify.nonNull(annotationType, "annotationType");

            List<AnnotationCandidate<A>> candidates = new ArrayList<>();

            for (Class<?> type : ClassFinder.findAll(Matcher.constant(true), baseClasses)) {
                collectTypeCandidate(annotationType, type, candidates);
                collectMethodCandidates(annotationType, type, candidates);
                collectFieldCandidates(annotationType, type, candidates);
            }

            return List.copyOf(candidates);
        }

        /**
         * Collects type-level candidate.
         */
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

        /**
         * Collects method-level candidates.
         */
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

        /**
         * Collects field-level candidates.
         */
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