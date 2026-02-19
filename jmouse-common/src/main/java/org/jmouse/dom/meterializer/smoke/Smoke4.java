package org.jmouse.dom.meterializer.smoke;

import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.dom.meterializer.RenderingPipeline;
import org.jmouse.meterializer.TemplateRegistry;
import org.jmouse.meterializer.build.Include;
import org.jmouse.dom.meterializer.BootstrapTemplates;
import org.jmouse.dom.meterializer.modules.BootstrapThemeModule;
import org.jmouse.dom.meterializer.modules.ThemeAssembly;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jmouse.meterializer.NodeTemplate.element;
import static org.jmouse.meterializer.NodeTemplate.text;
import static org.jmouse.meterializer.ValueExpression.constant;
import static org.jmouse.meterializer.ValueExpression.path;

public final class Smoke4 {

    public static void main(String[] args) {
        ObjectAccessorWrapper accessorWrapper = new ObjectAccessorWrapper();

        ThemeAssembly    bootstrapAssembly = ThemeAssembly.forTheme(new BootstrapThemeModule());
        TemplateRegistry registry          = bootstrapAssembly.catalog();

        registerTemplates(registry);

        RenderingPipeline pipeline = bootstrapAssembly.build(accessorWrapper);

        Map<String, Object> model = demoModel();

        Node node = pipeline.render("smoke4/form", model);

        node.execute(NodeContext.REORDER_NODE_CORRECTOR);

        System.out.println(node.interpret(NodeContext.defaults()));
    }

    private static void registerTemplates(TemplateRegistry registry) {
        registry.register("smoke4/form", Templates.form());
        registry.register("smoke4/field/text", BootstrapTemplates.inputText("name", "label", "value"));
        registry.register("smoke4/field/select", BootstrapTemplates.select(
                "name",
                "label",
                "options",
                "option.key",
                "option.label"
        ));
        registry.register("smoke4/button/submit", BootstrapTemplates.submitButton("SUBMIT!!!"));
    }

    private static Map<String, Object> demoModel() {
        Map<String, Object> model = new LinkedHashMap<>();

        model.put("title", "Add Resistor!");
        model.put("description", "Minimal smoke form.");

        // text input
        model.put("nameField", Map.of(
                "name", "element_name",
                "label", "Name Of Component:",
                "value", "R12"
        ));

        // select
        model.put("vendorField", Map.of(
                "name", "vendor",
                "label", "Vendor:",
                "selected", "ti",
                "options", List.of(
                        Map.of("key", "vishay", "label", "Vishay"),
                        Map.of("key", "ti", "label", "Texas Instruments")
                )
        ));

        // submit
        model.put("submit", Map.of(
                "caption", "Submit"
        ));

        return model;
    }

    private static final class Templates {

        private Templates() {}

        static NodeTemplate form() {
            return element("form", form -> form
                    .attribute("method", constant("POST"))
                    .attribute("class", constant("p-3"))

                    .child(element("h3", h ->
                            h.child(text(path("title")))
                    ))

                    .child(element("p", p -> p
                            .attribute("class", constant("text-muted"))
                            .child(text(path("description")))
                    ))

                    // text input include
                    .child(Include.template(
                            constant("smoke4/field/text"),
                            path("nameField")
                    ))

                    // select include
                    .child(Include.template(
                            constant("smoke4/field/select"),
                            path("vendorField")
                    ))

                    // submit include
                    .child(Include.template(
                            constant("smoke4/button/submit"),
                            path("submit")
                    ))
            );
        }

    }
}