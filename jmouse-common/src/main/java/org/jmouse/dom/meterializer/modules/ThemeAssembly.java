package org.jmouse.dom.meterializer.modules;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.dom.Node;
import org.jmouse.dom.meterializer.DOMMaterializer;
import org.jmouse.dom.meterializer.RenderingPipeline;
import org.jmouse.meterializer.*;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.util.ArrayList;
import java.util.List;

public final class ThemeAssembly {

    private final ThemeModule themeModule;

    private final List<RenderingHook<Node>>                  hooks        = new ArrayList<>();
    private final TemplateRegistry                     templateRegistry;
    private final List<ThemeModule.OrderedTransformer> transformers = new ArrayList<>();

    private ThemeAssembly(ThemeModule themeModule) {
        this.themeModule = Verify.nonNull(themeModule, "themeModule");
        TemplateRegistry baseCatalog = TemplateRegistry.create();
        this.templateRegistry = TemplateRegistry.overlay(baseCatalog);
        themeModule.contributeBlueprints(baseCatalog);
    }

    public static ThemeAssembly forTheme(ThemeModule themeModule) {
        return new ThemeAssembly(themeModule);
    }

    public ThemeAssembly overrideBlueprint(String key, NodeTemplate blueprint) {
        templateRegistry.register(key, blueprint);
        return this;
    }

    public ThemeAssembly addTransformer(int order, TemplateTransformer transformer) {
        transformers.add(new ThemeModule.OrderedTransformer(order, transformer));
        return this;
    }

    public ThemeAssembly addHook(RenderingHook<Node> hook) {
        hooks.add(hook);
        return this;
    }

    @SuppressWarnings("unchecked")
    public RenderingPipeline build(AccessorWrapper accessorWrapper) {
        Verify.nonNull(accessorWrapper, "accessorWrapper");

        PipelineBuilder<Node, RenderingPipeline> builder = RenderingPipeline.builder()
                .templateRegistry(templateRegistry)
                .accessorWrapper(accessorWrapper);

        for (ThemeModule.OrderedTransformer transformer : themeModule.transformers()) {
            builder.transformer(transformer.order(), transformer.transformer());
        }

        for (ThemeModule.OrderedTransformer transformer : transformers) {
            builder.transformer(transformer.order(), transformer.transformer());
        }

        for (RenderingHook<?> hook : themeModule.hooks()) {
            builder.hook((RenderingHook<Node>) hook);
        }

        for (RenderingHook<?> hook : hooks) {
            builder.hook((RenderingHook<Node>) hook);
        }

        builder.materializer(new DOMMaterializer());

        return builder.build();
    }

    public TemplateRegistry catalog() {
        return templateRegistry;
    }
}
