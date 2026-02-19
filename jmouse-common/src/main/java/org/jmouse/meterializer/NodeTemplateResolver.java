package org.jmouse.meterializer;

import org.jmouse.core.CyclicReferenceDetector;
import org.jmouse.core.DefaultCyclicReferenceDetector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.core.Verify.nonNull;

public interface NodeTemplateResolver {

    /**
     * Resolve blueprint by key in the given execution context.
     * Returned blueprint is "compiled": catalog + transformers already applied.
     */
    NodeTemplate resolve(String key, RenderingExecution execution);

    static NodeTemplateResolver of(TemplateRegistry templateRegistry, TransformerChain transformerChain) {
        nonNull(templateRegistry, "templateRegistry");
        nonNull(transformerChain, "transformerChain");
        return new Implementation(templateRegistry, transformerChain);
    }

    final class Implementation implements NodeTemplateResolver {

        private static final IllegalStateException CIRCULAR =
                new IllegalStateException("Circular blueprint key reference");

        private final TemplateRegistry                templateRegistry;
        private final TransformerChain                transformerChain;
        private final Map<String, NodeTemplate>       cache    = new ConcurrentHashMap<>();
        private final CyclicReferenceDetector<String> detector = new DefaultCyclicReferenceDetector<>();

        public Implementation(TemplateRegistry templateRegistry, TransformerChain transformerChain) {
            this.templateRegistry = nonNull(templateRegistry, "catalog");
            this.transformerChain = nonNull(transformerChain, "transformerChain");
        }

        @Override
        public NodeTemplate resolve(String key, RenderingExecution execution) {
            nonNull(key, "key");
            nonNull(execution, "execution");

            NodeTemplate cached = cache.get(key);

            if (cached != null) {
                return cached;
            }

            detector.detect(() -> key, () -> CIRCULAR);

            try {
                NodeTemplate resolved = templateRegistry.resolve(key);
                NodeTemplate compiled = transformerChain.apply(resolved, execution);
                cache.put(key, compiled);
                return compiled;
            } finally {
                detector.remove(() -> key);
            }
        }
    }

}
