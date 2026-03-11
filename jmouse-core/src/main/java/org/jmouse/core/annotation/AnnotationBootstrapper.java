package org.jmouse.core.annotation;

import org.jmouse.core.Verify;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Bootstraps annotation processors against scan roots. 🚀
 *
 * <p>
 * Coordinates annotation discovery and processor execution.
 * For each registered {@link AnnotationProcessor}, the bootstrapper
 * finds matching candidates and delegates them to the processor.
 * </p>
 */
public interface AnnotationBootstrapper {

    /**
     * Applies all processors to discovered candidates.
     *
     * @param context     processing context
     * @param processors  processors to execute
     * @param baseClasses scan roots
     */
    void bootstrap(
            AnnotationProcessingContext context,
            Iterable<? extends AnnotationProcessor<?>> processors,
            Class<?>... baseClasses
    );

    /**
     * Returns default bootstrapper implementation.
     *
     * @param discovery annotation discovery strategy
     *
     * @return default bootstrapper
     */
    static AnnotationBootstrapper defaults(AnnotationDiscovery discovery) {
        return new Default(discovery);
    }

    /**
     * Default {@link AnnotationBootstrapper} implementation. 🧱
     */
    class Default implements AnnotationBootstrapper {

        private final AnnotationDiscovery discovery;

        public Default(AnnotationDiscovery discovery) {
            this.discovery = Verify.nonNull(discovery, "discovery");
        }

        /**
         * Runs all processors against discovered annotation candidates.
         */
        @Override
        public void bootstrap(
                AnnotationProcessingContext context,
                Iterable<? extends AnnotationProcessor<?>> processors,
                Class<?>... baseClasses
        ) {
            Verify.nonNull(context, "context");
            Verify.nonNull(processors, "processors");

            for (AnnotationProcessor<?> processor : processors) {
                processProcessor(context, processor, baseClasses);
            }
        }

        /**
         * Discovers and processes candidates for a single processor.
         */
        private <A extends Annotation> void processProcessor(
                AnnotationProcessingContext context,
                AnnotationProcessor<A> processor,
                Class<?>... baseClasses
        ) {
            List<AnnotationCandidate<A>> candidates = discovery.findCandidates(processor.annotationType(), baseClasses);

            for (AnnotationCandidate<A> candidate : candidates) {
                processor.process(candidate, context);
            }
        }
    }

}