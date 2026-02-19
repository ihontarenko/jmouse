package org.jmouse.dom.meterializer.modules;

import org.jmouse.meterializer.TemplateRegistry;
import org.jmouse.meterializer.TemplateTransformer;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.util.List;

public interface ThemeModule {

    void contributeBlueprints(TemplateRegistry catalog);

    List<OrderedTransformer> transformers();

    List<RenderingHook<?>> hooks();

    record OrderedTransformer(int order, TemplateTransformer transformer) {}

}
