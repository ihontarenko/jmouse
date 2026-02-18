package org.jmouse.dom.blueprint.transform;

import org.jmouse.dom.blueprint.BlueprintTransformer;

/**
 * Html5-oriented transformer set.
 */
public final class Html5Transformers {

    private Html5Transformers() {}

    public static BlueprintTransformer create() {
        return RuleBasedBlueprintTransformer.builder()
                // Example: ensure <label for="..."> has no extra changes by default
                .build();
    }
}
