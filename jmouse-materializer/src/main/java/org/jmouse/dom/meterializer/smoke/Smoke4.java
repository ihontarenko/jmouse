package org.jmouse.dom.meterializer.smoke;

import org.jmouse.core.access.ObjectAccessorWrapper;
//import org.jmouse.dom.Node;
//import org.jmouse.dom.NodeContext;
//import org.jmouse.dom.meterializer.DOMMaterializer;
//import org.jmouse.dom.meterializer.DOMRenderingPipeline;
import org.jmouse.dom.meterializer.BootstrapTemplates;
import org.jmouse.dom.meterializer.BootstrapThemeModule;
import org.jmouse.meterializer.*;
import org.jmouse.xml.XmlPrinter;
import org.jmouse.xml.materializer.XmlDomMaterializer;
import org.jmouse.xml.materializer.XmlRenderingPipeline;
import org.jmouse.xml.materializer.XmlThemeModule;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

public final class Smoke4 {

    public static void main(String[] args) {
        ThemeAssembly<org.w3c.dom.Node, XmlRenderingPipeline> assembler = ThemeAssembly.forTheme(new XmlThemeModule());
        PipelineBuilder<org.w3c.dom.Node, XmlRenderingPipeline>           builder   = new PipelineBuilder<>();

        registerTemplates(assembler.templateRegistry());
        builder.instanceFactory(XmlRenderingPipeline::new);

        ValueResolver valueResolver = new DefaultValueResolver(new PathValueResolver());

        XmlRenderingPipeline pipeline = assembler.build(new ObjectAccessorWrapper(), builder, new XmlDomMaterializer(valueResolver));

        Map<String, Object> model = DemoModels.bootstrapFormDemo();

        Node node = pipeline.render("smoke4/form", model);

        XmlPrinter.toString(node);

//        node.execute(NodeContext.CORRECT_NODE_DEPTH);
//
//        Node text = pipeline.render("field/select", Map.of(
//                "name", "vendor",
//                "label", "Vendor:",
//                "selected", "ti",
//                "options", List.of(
//                        Map.of("key", "vishay", "label", "Vishay"),
//                        Map.of("key", "ti", "label", "Texas Instruments")
//                )
//        ));
//
//        text.execute(NodeContext.CORRECT_NODE_DEPTH);
//
//        System.out.println(node.interpret(NodeContext.defaults()));
//        System.out.println(text.interpret(NodeContext.defaults()));
    }

    private static void registerTemplates(TemplateRegistry registry) {
        registry.register("smoke4/form", Templates.bootstrapForm());

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