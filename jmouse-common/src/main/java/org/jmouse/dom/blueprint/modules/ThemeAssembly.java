package org.jmouse.dom.blueprint.modules;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.dom.blueprint.*;
import org.jmouse.dom.blueprint.hooks.RenderingHook;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a pipeline from a theme module and user overrides.
 */
public final class ThemeAssembly {

    private final ThemeModule themeModule;

    private final List<RenderingHook>                  hooks        = new ArrayList<>();
    private final List<ThemeModule.OrderedTransformer> transformers = new ArrayList<>();
    private final BlueprintCatalog                     catalog;

    private ThemeAssembly(ThemeModule themeModule) {
        this.themeModule = Verify.nonNull(themeModule, "themeModule");
        BlueprintCatalog baseCatalog = BlueprintCatalog.create();
        this.catalog = BlueprintCatalog.overlay(baseCatalog);
        themeModule.contributeBlueprints(baseCatalog);
    }

    public static ThemeAssembly forTheme(ThemeModule themeModule) {
        return new ThemeAssembly(themeModule);
    }

    /**
     * Registers or overrides a blueprint by key.
     */
    public ThemeAssembly overrideBlueprint(String key, Blueprint blueprint) {
        catalog.register(key, blueprint);
        return this;
    }

    /**
     * Adds a transformer with an explicit order.
     */
    public ThemeAssembly addTransformer(int order, BlueprintTransformer transformer) {
        transformers.add(new ThemeModule.OrderedTransformer(order, transformer));
        return this;
    }

    /**
     * Adds a hook.
     */
    public ThemeAssembly addHook(RenderingHook hook) {
        hooks.add(hook);
        return this;
    }

    public RenderingPipeline build(AccessorWrapper accessorWrapper) {
        Verify.nonNull(accessorWrapper, "accessorWrapper");

        RenderingPipeline.Builder builder = RenderingPipeline.builder().catalog(catalog)
                .accessorWrapper(accessorWrapper);

        for (ThemeModule.OrderedTransformer transformer : themeModule.transformers()) {
            builder.transformer(transformer.order(), transformer.transformer());
        }

        for (ThemeModule.OrderedTransformer transformer : transformers) {
            builder.transformer(transformer.order(), transformer.transformer());
        }

        for (RenderingHook hook : themeModule.hooks()) {
            builder.hook(hook);
        }

        for (RenderingHook hook : hooks) {
            builder.hook(hook);
        }

        builder.materializer(new BlueprintMaterializer.Implementation(catalog));

        return builder.build();
    }

    public BlueprintCatalog catalog() {
        return catalog;
    }
}
