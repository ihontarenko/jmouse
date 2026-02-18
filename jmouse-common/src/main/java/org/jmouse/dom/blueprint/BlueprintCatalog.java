package org.jmouse.dom.blueprint;

import java.util.Objects;

/**
 * Catalog that resolves blueprints by key.
 */
public interface BlueprintCatalog {

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
     * @param key blueprint key
     * @param blueprint blueprint
     */
    void register(String key, Blueprint blueprint);

    static BlueprintCatalog create() {
        return new DefaultBlueprintCatalog();
    }

    static BlueprintCatalog overlay(BlueprintCatalog base) {
        return new OverlayBlueprintCatalog(base);
    }
}
