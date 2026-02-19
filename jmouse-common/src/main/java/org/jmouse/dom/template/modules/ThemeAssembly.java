package org.jmouse.dom.template.modules;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.dom.template.*;
import org.jmouse.dom.template.hooks.RenderingHook;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a pipeline from a theme module and user overrides.
 */
public final class ThemeAssembly {

    private final ThemeModule themeModule;

    private final List<RenderingHook>                  hooks        = new ArrayList<>();
    private final List<ThemeModule.OrderedTransformer> transformers = new ArrayList<>();
    private final TemplateRegistry                     catalog;

    private ThemeAssembly(ThemeModule themeModule) {
        this.themeModule = Verify.nonNull(themeModule, "themeModule");
        TemplateRegistry baseCatalog = TemplateRegistry.create();
        this.catalog = TemplateRegistry.overlay(baseCatalog);
        themeModule.contributeBlueprints(baseCatalog);
    }

    public static ThemeAssembly forTheme(ThemeModule themeModule) {
        return new ThemeAssembly(themeModule);
    }

    /**
     * Registers or overrides a blueprint by key.
     */
    public ThemeAssembly overrideBlueprint(String key, NodeTemplate blueprint) {
        catalog.register(key, blueprint);
        return this;
    }

    /**
     * Adds a transformer with an explicit order.
     */
    public ThemeAssembly addTransformer(int order, TemplateTransformer transformer) {
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

        builder.materializer(new TemplateMaterializer.Implementation(catalog));

        return builder.build();
    }

    public TemplateRegistry catalog() {
        return catalog;
    }
}
