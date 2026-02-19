package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog that resolves blueprints by key.
 */
public interface BlueprintCatalog {

    static BlueprintCatalog create() {
        return new Implementation();
    }

    static BlueprintCatalog overlay(BlueprintCatalog base) {
        return new Overlay(base);
    }

    /**
     * Resolve a blueprint by key.
     *
     * @param key blueprint key
     * @return blueprint
     */
    Blueprint resolve(String key);

    /**
     * Register a blueprint under the given key.
     *
     * @param key       blueprint key
     * @param blueprint blueprint
     */
    void register(String key, Blueprint blueprint);

    final class Implementation implements BlueprintCatalog {

        private final Map<String, Blueprint> storage = new LinkedHashMap<>();

        @Override
        public Blueprint resolve(String key) {
            Verify.nonNull(key, "key");
            Blueprint blueprint = storage.get(key);
            if (blueprint == null) {
                throw new IllegalStateException("No blueprint registered for key: " + key);
            }
            return blueprint;
        }

        @Override
        public void register(String key, Blueprint blueprint) {
            Verify.nonNull(key, "key");
            Verify.nonNull(blueprint, "blueprint");
            storage.put(key, blueprint);
        }
    }

    final class Overlay implements BlueprintCatalog {

        private final BlueprintCatalog       base;
        private final Map<String, Blueprint> local = new LinkedHashMap<>();

        public Overlay(BlueprintCatalog base) {
            this.base = Verify.nonNull(base, "base");
        }

        @Override
        public Blueprint resolve(String key) {
            Verify.nonNull(key, "key");
            Blueprint blueprint = local.get(key);
            if (blueprint != null) {
                return blueprint;
            }
            return base.resolve(key);
        }

        @Override
        public void register(String key, Blueprint blueprint) {
            Verify.nonNull(key, "key");
            Verify.nonNull(blueprint, "blueprint");
            local.put(key, blueprint);
        }

        public BlueprintCatalog base() {
            return base;
        }
    }

}
