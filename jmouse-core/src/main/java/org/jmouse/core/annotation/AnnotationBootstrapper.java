package org.jmouse.core.annotation;

import org.jmouse.core.Verify;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Bootstraps annotation processors against scan roots. 🚀
 */
public interface AnnotationBootstrapper {

    /**
     * Applies all registered processors to discovered candidates.
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
     * Default {@link AnnotationBootstrapper} implementation. 🧱
     */
    class Default implements AnnotationBootstrapper {

        private final AnnotationDiscovery discovery;

        public Default(AnnotationDiscovery discovery) {
            this.discovery = Verify.nonNull(discovery, "discovery");
        }

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