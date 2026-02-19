package org.jmouse.dom.meterializer.modules;

import org.jmouse.template.TemplateRegistry;
import org.jmouse.template.TemplateTransformer;
import org.jmouse.template.hooks.RenderingHook;

import java.util.List;

/**
 * Contributes standard building blocks: blueprints, transformers, hooks.
 */
public interface ThemeModule {

    /**
     * Register module blueprints.
     */
    void contributeBlueprints(TemplateRegistry catalog);

    /**
     * Provide module transformers ordered by pipeline order.
     */
    List<OrderedTransformer> transformers();

    /**
     * Provide module hooks.
     */
    List<RenderingHook> hooks();

    record OrderedTransformer(int order, TemplateTransformer transformer) {}
}
