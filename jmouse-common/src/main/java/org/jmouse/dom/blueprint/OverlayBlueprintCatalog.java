package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog that overlays local registrations on top of a base catalog.
 */
public final class OverlayBlueprintCatalog implements BlueprintCatalog {

    private final BlueprintCatalog       base;
    private final Map<String, Blueprint> local = new LinkedHashMap<>();

    public OverlayBlueprintCatalog(BlueprintCatalog base) {
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
