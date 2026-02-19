package org.jmouse.dom.meterializer.modules;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.TemplateRegistry;
import org.jmouse.meterializer.TemplateTransformer;
import org.jmouse.meterializer.ThemeModule;
import org.jmouse.meterializer.hooks.RenderingHook;
import org.jmouse.meterializer.transform.BootstrapTransformers;

import java.util.List;

public final class BootstrapThemeModule implements ThemeModule<Node> {

    @Override
    public void contributeBlueprints(TemplateRegistry templateRegistry) {

    }

    @Override
    public List<OrderedTransformer> transformers() {
        TemplateTransformer transformer = BootstrapTransformers.create();
        return List.of(new OrderedTransformer(100, transformer));
    }

    @Override
    public List<RenderingHook<Node>> hooks() {
        return List.of();
    }
}
