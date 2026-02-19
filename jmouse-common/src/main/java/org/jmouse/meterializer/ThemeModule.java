package org.jmouse.meterializer;

import org.jmouse.meterializer.hooks.RenderingHook;

import java.util.List;

public interface ThemeModule<T> {

    void contributeBlueprints(TemplateRegistry templateRegistry);

    List<OrderedTransformer> transformers();

    List<RenderingHook<T>> hooks();

    record OrderedTransformer(int order, TemplateTransformer transformer) {}

}
