package org.jmouse.dom.meterializer.smoke;

import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.dom.CorrectNodeDepth;
import org.jmouse.dom.Node;
import org.jmouse.dom.meterializer.DOMMaterializer;
import org.jmouse.dom.meterializer.DOMRenderingPipeline;
import org.jmouse.dom.meterializer.BootstrapTemplates;
import org.jmouse.dom.meterializer.BootstrapThemeModule;
import org.jmouse.dom.renderer.RendererContext;
import org.jmouse.dom.renderer.Renderers;
import org.jmouse.dom.renderer.RenderingProcessor;
import org.jmouse.meterializer.*;

import java.util.Map;

public final class Smoke4 {

    public static void main(String[] args) {
        ThemeAssembly<Node, DOMRenderingPipeline> assembler = ThemeAssembly.forTheme(new BootstrapThemeModule());
        PipelineBuilder<Node, DOMRenderingPipeline>           builder   = new PipelineBuilder<>();

        registerTemplates(assembler.templateRegistry());
        builder.instanceFactory(DOMRenderingPipeline::new);

        ValueResolver valueResolver = new DefaultValueResolver(new PathValueResolver());

        DOMRenderingPipeline pipeline = assembler.build(new ObjectAccessorWrapper(), builder, new DOMMaterializer(valueResolver));

        Map<String, Object> model = DemoModels.bootstrapFormDemo();

        model.put("submitCaption", "Submit Demo!");

        Node node = pipeline.render("smoke4/form", model, r -> r.putAttribute("submitCaption2", "Submit Request!"));

        node.execute(new CorrectNodeDepth());

        RenderingProcessor engine = new RenderingProcessor(Renderers.html());
        String             out    = engine.render(node, RendererContext.defaultsHtmlPretty());

        System.out.println(out);
    }

    private static void registerTemplates(TemplateRegistry registry) {
        registry.register("smoke4/form", Templates.defaultForm());

        registry.register("field/text",
                          BootstrapTemplates.inputText("name", "label", "value"));

        registry.register("field/select",
                          BootstrapTemplates.select(
                                  "name",
                                  "label",
                                  "options",
                                  "option.key",
                                  "option.label"
                          ));

        registry.register("uni/button/submit",
                          BootstrapTemplates.submitButton("SUBMIT!!!"));
    }

}