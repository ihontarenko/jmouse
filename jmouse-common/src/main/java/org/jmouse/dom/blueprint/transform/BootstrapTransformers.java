package org.jmouse.dom.blueprint.transform;

import org.jmouse.dom.blueprint.BlueprintTransformer;

import static org.jmouse.dom.blueprint.transform.Change.*;
import static org.jmouse.dom.blueprint.transform.Match.*;

/**
 * Bootstrap-oriented transformer set.
 */
public final class BootstrapTransformers {

    private BootstrapTransformers() {}

    public static BlueprintTransformer create() {
        return RuleBasedBlueprintTransformer.builder()

                .rule(100, tagName("input"), addClass("form-control " + BootstrapTransformers.class.getSimpleName()))
                .rule(100, tagName("select"), addClass("form-select"))
                .rule(100, tagName("textarea"), addClass("form-control"))
                .rule(100, tagName("label"), addClass("form-label bootstrap-label"))
                .rule(100, tagName("form"), addClass("dynamic-form"))

                // Example: wrap select into spacing block
                .rule(50, tagName("select"),
                      wrapWith("div", addClass("mb-3 extended-select")))

                .build();
    }
}
