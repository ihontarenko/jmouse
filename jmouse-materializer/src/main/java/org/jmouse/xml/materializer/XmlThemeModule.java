package org.jmouse.xml.materializer;

import org.jmouse.meterializer.TemplateRegistry;
import org.jmouse.meterializer.ThemeModule;
import org.jmouse.meterializer.hooks.RenderingHook;
import org.w3c.dom.Node;

import java.util.List;

public class XmlThemeModule implements ThemeModule<Node> {

    @Override
    public void contributeBlueprints(TemplateRegistry templateRegistry) {

    }

    @Override
    public List<OrderedTransformer> transformers() {
        return List.of();
    }

    @Override
    public List<RenderingHook<Node>> hooks() {
        return List.of();
    }
}
