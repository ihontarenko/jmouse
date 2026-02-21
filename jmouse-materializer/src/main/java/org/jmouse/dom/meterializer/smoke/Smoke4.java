package org.jmouse.dom.meterializer.smoke;

import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.meterializer.DOMMaterializer;
import org.jmouse.dom.meterializer.DOMRenderingPipeline;
import org.jmouse.dom.meterializer.BootstrapTemplates;
import org.jmouse.dom.meterializer.BootstrapThemeModule;
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

        node.execute(NodeContext.CORRECT_NODE_DEPTH);

        System.out.println(node.interpret(NodeContext.defaults()));
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