package org.jmouse.dom.meterializer.modules;

import org.jmouse.template.TemplateRegistry;
import org.jmouse.template.TemplateTransformer;
import org.jmouse.template.hooks.RenderingHook;
import org.jmouse.template.transform.BootstrapTransformers;
import org.jmouse.template.defaults.StandardBlueprints;

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
    public List<RenderingHook> hooks() {
        return List.of();
    }
}
