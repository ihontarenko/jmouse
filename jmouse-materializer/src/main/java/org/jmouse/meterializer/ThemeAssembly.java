package org.jmouse.meterializer;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.Verify.nonNull;

public final class ThemeAssembly<T, R extends Rendering<T>> {

    private final ThemeModule<T> themeModule;

    private final List<RenderingHook<T>>                  hooks        = new ArrayList<>();
    private final TemplateRegistry                     templateRegistry;
    private final List<ThemeModule.OrderedTransformer> transformers = new ArrayList<>();

    private ThemeAssembly(ThemeModule<T> themeModule) {
        this.themeModule = nonNull(themeModule, "themeModule");
        TemplateRegistry baseCatalog = TemplateRegistry.create();
        this.templateRegistry = TemplateRegistry.overlay(baseCatalog);
        themeModule.contributeTemplates(baseCatalog);
    }

    public static <T, R extends Rendering<T>> ThemeAssembly<T, R> forTheme(ThemeModule<T> themeModule) {
        return new ThemeAssembly<>(themeModule);
    }

    public ThemeAssembly<T, R> overrideBlueprint(String key, NodeTemplate blueprint) {
        templateRegistry.register(key, blueprint);
        return this;
    }

    public ThemeAssembly<T, R> addTransformer(int order, TemplateTransformer transformer) {
        transformers.add(new ThemeModule.OrderedTransformer(order, transformer));
        return this;
    }

    public ThemeAssembly<T, R> addHook(RenderingHook<T> hook) {
        hooks.add(hook);
        return this;
    }

    public R build(
            AccessorWrapper accessorWrapper,
            PipelineBuilder<T, R> builderInstance,
            TemplateMaterializer<T> materializer
    ) {
        nonNull(accessorWrapper, "accessorWrapper");

        PipelineBuilder<T, R> builder = builderInstance
                .templateRegistry(templateRegistry)
                .accessorWrapper(accessorWrapper);

        for (ThemeModule.OrderedTransformer transformer : transformers) {
            builder.transformer(transformer.order(), transformer.transformer());
        }

        for (ThemeModule.OrderedTransformer transformer : themeModule.transformers()) {
            builder.transformer(transformer.order(), transformer.transformer());
        }

        for (RenderingHook<T> hook : hooks) {
            builder.hook(hook);
        }

        for (RenderingHook<T> hook : themeModule.hooks()) {
            builder.hook(hook);
        }


        builder.materializer(materializer);

        return builder.build();
    }

    public TemplateRegistry templateRegistry() {
        return templateRegistry;
    }
}
