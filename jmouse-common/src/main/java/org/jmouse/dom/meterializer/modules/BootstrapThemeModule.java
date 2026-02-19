package org.jmouse.dom.meterializer.modules;

import org.jmouse.meterializer.TemplateRegistry;
import org.jmouse.meterializer.TemplateTransformer;
import org.jmouse.meterializer.hooks.RenderingHook;
import org.jmouse.meterializer.transform.BootstrapTransformers;
import org.jmouse.dom.meterializer.StandardBlueprints;

import java.util.List;

public final class BootstrapThemeModule implements ThemeModule {

    @Override
    public void contributeBlueprints(TemplateRegistry catalog) {
        catalog.register("standard/form", StandardBlueprints.form());
    }

    @Override
    public List<OrderedTransformer> transformers() {
        TemplateTransformer transformer = BootstrapTransformers.create();
        return List.of(new OrderedTransformer(100, transformer));
    }

    @Override
    public List<RenderingHook<?>> hooks() {
        return List.of();
    }
}
