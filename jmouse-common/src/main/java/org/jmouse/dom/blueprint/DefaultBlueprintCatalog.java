package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default in-memory blueprint catalog.
 */
public final class DefaultBlueprintCatalog implements BlueprintCatalog {

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
