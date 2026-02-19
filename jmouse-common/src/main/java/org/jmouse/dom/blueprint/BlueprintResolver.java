package org.jmouse.dom.blueprint;

import org.jmouse.core.CyclicReferenceDetector;
import org.jmouse.core.DefaultCyclicReferenceDetector;
import org.jmouse.core.Verify;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface BlueprintResolver {

    /**
     * Resolve blueprint by key in the given execution context.
     * Returned blueprint is "compiled": catalog + transformers already applied.
     */
    Blueprint resolve(String key, RenderingExecution execution);

    static BlueprintResolver of(BlueprintCatalog catalog, BlueprintTransformerChain chain) {
        Verify.nonNull(catalog, "catalog");
        Verify.nonNull(chain, "chain");
        return new Implementation(catalog, chain);
    }

    final class Implementation implements BlueprintResolver {

        private static final IllegalStateException CIRCULAR =
                new IllegalStateException("Circular blueprint key reference");

        private final BlueprintCatalog                catalog;
        private final BlueprintTransformerChain       transformerChain;
        private final Map<String, Blueprint>          cache    = new ConcurrentHashMap<>();
        private final CyclicReferenceDetector<String> detector = new DefaultCyclicReferenceDetector<>();

        public Implementation(BlueprintCatalog catalog, BlueprintTransformerChain transformerChain) {
            this.catalog = Verify.nonNull(catalog, "catalog");
            this.transformerChain = Verify.nonNull(transformerChain, "transformerChain");
        }

        @Override
        public Blueprint resolve(String key, RenderingExecution execution) {
            Verify.nonNull(key, "key");
            Verify.nonNull(execution, "execution");

            Blueprint cached = cache.get(key);

            if (cached != null) {
                return cached;
            }

            detector.detect(() -> key, () -> CIRCULAR);

            try {
                Blueprint resolved = catalog.resolve(key);
                Blueprint compiled = transformerChain.apply(resolved, execution);
                cache.put(key, compiled);
                return compiled;
            } finally {
                detector.remove(() -> key);
            }
        }
    }

}
