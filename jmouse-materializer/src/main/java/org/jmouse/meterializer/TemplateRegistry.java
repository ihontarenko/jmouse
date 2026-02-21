package org.jmouse.meterializer;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog that resolves templates by key.
 */
public interface TemplateRegistry {

    static TemplateRegistry create() {
        return new Implementation();
    }

    static TemplateRegistry overlay(TemplateRegistry base) {
        return new Overlay(base);
    }

    /**
     * Resolve a template by key.
     *
     * @param key template key
     * @return template
     */
    NodeTemplate resolve(String key);

    /**
     * Register a template under the given key.
     *
     * @param key       template key
     * @param template template
     */
    void register(String key, NodeTemplate template);

    final class Implementation implements TemplateRegistry {

        private final Map<String, NodeTemplate> storage = new LinkedHashMap<>();

        @Override
        public NodeTemplate resolve(String key) {
            Verify.nonNull(key, "key");
            NodeTemplate template = storage.get(key);
            if (template == null) {
                throw new IllegalStateException("No template registered for key: " + key);
            }
            return template;
        }

        @Override
        public void register(String key, NodeTemplate template) {
            Verify.nonNull(key, "key");
            Verify.nonNull(template, "template");
            storage.put(key, template);
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
            NodeTemplate template = local.get(key);
            if (template != null) {
                return template;
            }
            return base.resolve(key);
        }

        @Override
        public void register(String key, NodeTemplate template) {
            Verify.nonNull(key, "key");
            Verify.nonNull(template, "template");
            local.put(key, template);
        }

        public TemplateRegistry base() {
            return base;
        }
    }

}
