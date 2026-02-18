package org.jmouse.dom.blueprint.modules;

import org.jmouse.dom.blueprint.BlueprintCatalog;
import org.jmouse.dom.blueprint.BlueprintTransformer;
import org.jmouse.dom.blueprint.hooks.RenderingHook;
import org.jmouse.dom.blueprint.standard.StandardBlueprints;
import org.jmouse.dom.blueprint.transform.Html5Transformers;

import java.util.List;

public final class Html5ThemeModule implements ThemeModule {

    @Override
    public void contributeBlueprints(BlueprintCatalog catalog) {
        catalog.register("standard/form", StandardBlueprints.form());
    }

    @Override
    public List<OrderedTransformer> transformers() {
        BlueprintTransformer transformer = Html5Transformers.create();
        return List.of(new OrderedTransformer(100, transformer));
    }

    @Override
    public List<RenderingHook> hooks() {
        return List.of();
    }
}
