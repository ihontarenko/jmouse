package org.jmouse.dom.blueprint.modules;

import org.jmouse.dom.blueprint.BlueprintCatalog;
import org.jmouse.dom.blueprint.BlueprintTransformer;
import org.jmouse.dom.blueprint.hooks.RenderingHook;

import java.util.List;

/**
 * Contributes standard building blocks: blueprints, transformers, hooks.
 */
public interface ThemeModule {

    /**
     * Register module blueprints.
     */
    void contributeBlueprints(BlueprintCatalog catalog);

    /**
     * Provide module transformers ordered by pipeline order.
     */
    List<OrderedTransformer> transformers();

    /**
     * Provide module hooks.
     */
    List<RenderingHook> hooks();

    record OrderedTransformer(int order, BlueprintTransformer transformer) {}
}
