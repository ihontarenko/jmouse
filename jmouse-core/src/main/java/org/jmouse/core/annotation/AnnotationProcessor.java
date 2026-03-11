package org.jmouse.core.annotation;

import java.lang.annotation.Annotation;

/**
 * Strategy for processing discovered annotation candidates. 🎯
 *
 * <p>
 * Implementations encapsulate logic for handling a specific annotation type
 * discovered during annotation scanning. The processor receives an
 * {@link AnnotationCandidate} describing the annotated element together with
 * a {@link AnnotationProcessingContext} that provides access to the processing
 * environment (registry, services, configuration, etc.).
 * </p>
 *
 * <p>
 * Typical responsibilities include:
 * </p>
 * <ul>
 *     <li>Registering metadata derived from the annotation</li>
 *     <li>Creating descriptors or handlers</li>
 *     <li>Applying configuration to a module or subsystem</li>
 * </ul>
 *
 * <p>
 * Processors are usually registered in an annotation processing pipeline
 * and invoked only for the annotation type returned by {@link #annotationType()}.
 * </p>
 *
 * @param <A> annotation type handled by this processor
 */
public interface AnnotationProcessor<A extends Annotation> {

    /**
     * Returns the annotation type supported by this processor. 🧩
     *
     * <p>
     * The processing engine uses this value to route discovered
     * {@link AnnotationCandidate candidates} to the appropriate processor.
     * </p>
     *
     * @return supported annotation class
     */
    Class<A> annotationType();

    /**
     * Processes a discovered annotation candidate. ⚙️
     *
     * <p>
     * The candidate provides access to:
     * </p>
     * <ul>
     *     <li>the annotation instance</li>
     *     <li>the annotated element (class, method, field, etc.)</li>
     *     <li>additional metadata collected during scanning</li>
     * </ul>
     *
     * <p>
     * Implementations may use the {@link AnnotationProcessingContext}
     * to interact with registries, factories, or module infrastructure.
     * </p>
     *
     * @param candidate discovered annotation candidate
     * @param context   processing context
     */
    void process(AnnotationCandidate<A> candidate, AnnotationProcessingContext context);
}