package org.jmouse.dom.blueprint.modules;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.RenderingPipeline;
import org.jmouse.dom.blueprint.hooks.RenderingHook;

public final class ThemeInstaller {

    private ThemeInstaller() {}

    public static RenderingPipeline.Builder install(RenderingPipeline.Builder builder, ThemeModule module) {
        Verify.nonNull(builder, "builder");
        Verify.nonNull(module, "module");

        module.contributeBlueprints(builder.catalog());

        for (ThemeModule.OrderedTransformer transformer : module.transformers()) {
            builder.addTransformer(transformer.order(), transformer.transformer());
        }

        for (RenderingHook hook : module.hooks()) {
            builder.addHook(hook);
        }

        return builder;
    }
}
