package org.jmouse.dom.template.modules;

import org.jmouse.dom.template.TemplateRegistry;
import org.jmouse.dom.template.TemplateTransformer;
import org.jmouse.dom.template.hooks.RenderingHook;
import org.jmouse.dom.template.transform.BootstrapTransformers;
import org.jmouse.dom.template.standard.StandardBlueprints;

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
