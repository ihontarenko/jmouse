package org.jmouse.dom.template;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog that resolves blueprints by key.
 */
public interface TemplateRegistry {

    static TemplateRegistry create() {
        return new Implementation();
    }

    static TemplateRegistry overlay(TemplateRegistry base) {
        return new Overlay(base);
    }

    /**
     * Resolve a blueprint by key.
     *
     * @param key blueprint key
     * @return blueprint
     */
    NodeTemplate resolve(String key);

    /**
     * Register a blueprint under the given key.
     *
     * @param key       blueprint key
     * @param blueprint blueprint
     */
    void register(String key, NodeTemplate blueprint);

    final class Implementation implements TemplateRegistry {

        private final Map<String, NodeTemplate> storage = new LinkedHashMap<>();

        @Override
        public NodeTemplate resolve(String key) {
            Verify.nonNull(key, "key");
            NodeTemplate blueprint = storage.get(key);
            if (blueprint == null) {
                throw new IllegalStateException("No blueprint registered for key: " + key);
            }
            return blueprint;
        }

        @Override
        public void register(String key, NodeTemplate blueprint) {
            Verify.nonNull(key, "key");
            Verify.nonNull(blueprint, "blueprint");
            storage.put(key, blueprint);
        }
    }

    final class Overlay implements TemplateRegistry {

        private final TemplateRegistry          base;
        private final Map<String, NodeTemplate> local = new LinkedHashMap<>();

        public Overlay(TemplateRegistry base) {
            this.base = Verify.nonNull(base, "base");
        }

        @Override
        public NodeTemplate resolve(String key) {
            Verify.nonNull(key, "key");
            NodeTemplate blueprint = local.get(key);
            if (blueprint != null) {
                return blueprint;
            }
            return base.resolve(key);
        }

        @Override
        public void register(String key, NodeTemplate blueprint) {
            Verify.nonNull(key, "key");
            Verify.nonNull(blueprint, "blueprint");
            local.put(key, blueprint);
        }

        public TemplateRegistry base() {
            return base;
        }
    }

}
