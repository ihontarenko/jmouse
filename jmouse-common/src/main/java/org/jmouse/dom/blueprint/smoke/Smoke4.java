package org.jmouse.dom.blueprint.smoke;

import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.TagName;
import org.jmouse.dom.blueprint.*;
import org.jmouse.dom.blueprint.build.Blueprints;
import org.jmouse.dom.blueprint.build.Include;
import org.jmouse.dom.blueprint.hooks.RenderingHook;
import org.jmouse.dom.blueprint.hooks.SubmissionDecorationHook;
import org.jmouse.dom.blueprint.model.*;
import org.jmouse.dom.blueprint.modules.BootstrapThemeModule;
import org.jmouse.dom.blueprint.modules.ThemeAssembly;
import org.jmouse.dom.blueprint.presets.BootstrapPreset;
import org.jmouse.dom.node.ElementNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Smoke4 {

    public static void main(String[] args) {
        ObjectAccessorWrapper accessorWrapper = new ObjectAccessorWrapper();

        // ---------------------------------------------------------------------
        // 1) "Database" form structure (example)
        // ---------------------------------------------------------------------
        DbForm dbForm = new DbForm(
                "resistor",
                "Add Resistor!",
                List.of(
                        new DbField("element_name", "Name Of Component:", ElementType.TEXT, List.of()),
                        new DbField("resistance", "Value In Ohms:", ElementType.NUMBER, List.of()),
                        new DbField("vendor", "Vendor", ElementType.RADIO, List.of(
                                new DbOption("vishay", "Vishay"),
                                new DbOption("ti", "Texas Instruments")
                        ))
                )
        );

        // ---------------------------------------------------------------------
        // 2) Mapping: database structure -> stable contract (FormModel)
        // ---------------------------------------------------------------------
        FormModel formModel = ContractMapper.toContract(dbForm);

        // ---------------------------------------------------------------------
        // 3) Simulate POST submission: values + error on "resistance"
        // ---------------------------------------------------------------------
        RenderingRequest request = new RenderingRequest()
                .putAttribute(SubmissionState.REQUEST_ATTRIBUTE,
                           SubmissionState.of(
                                   Map.of(
                                           "element_name", "R12",
                                           "resistance", "abc",      // invalid number -> error below
                                           "vendor", "ti"
                                   ),
                                   Map.of(
                                           "resistance", "Resistance must be a number"
                                   )
                           )
                );

        // ---------------------------------------------------------------------
        // 4) Theme switch: Bootstrap vs HTML5
        // ---------------------------------------------------------------------

        // Bootstrap theme
        ThemeAssembly bootstrapAssembly = ThemeAssembly.forTheme(new BootstrapThemeModule())
                .addHook(new SubmissionDecorationHook())
                .addHook(new RadioCheckedHook());

        BlueprintCatalog bootstrapCatalog = bootstrapAssembly.catalog();
        registerBootstrapBlueprints(bootstrapCatalog);

        RenderingPipeline bootstrapPipeline = bootstrapAssembly.build(accessorWrapper);

        Node bootstrapNode = bootstrapPipeline.render("smoke4/form", formModel, r -> merge(r, request));
        bootstrapNode.execute(NodeContext.REORDER_NODE_CORRECTOR);

        System.out.println("\n=== BOOTSTRAP THEME ===");
        System.out.println(bootstrapNode.interpret(NodeContext.defaults()));

        // HTML5-like theme (no Bootstrap classes, simpler layout)
/*        ThemeAssembly htmlAssembly = ThemeAssembly.forTheme(new BootstrapThemeModule())
                .addHook(new SubmissionDecorationHook())
                .addHook(new RadioCheckedHook());

        BlueprintCatalog htmlCatalog = htmlAssembly.catalog();
        registerHtml5Blueprints(htmlCatalog);

        RenderingPipeline htmlPipeline = htmlAssembly.build(accessorWrapper);

        Node htmlNode = htmlPipeline.render("smoke4/form", formModel, r -> merge(r, request));
        htmlNode.execute(NodeContext.REORDER_NODE_CORRECTOR);

        System.out.println("\n=== HTML5 THEME ===");
        System.out.println(htmlNode.interpret(NodeContext.defaults()));*/
    }

    // ---------------------------------------------------------------------
    // Blueprints registration (theme-specific)
    // ---------------------------------------------------------------------

    private static void registerBootstrapBlueprints(BlueprintCatalog catalog) {
        catalog.register("smoke4/form", BootstrapBlueprints.form());
        catalog.register("smoke4/fragment", CommonBlueprints.fragment());

//        catalog.register("smoke4/control/input-text", BootstrapBlueprints.inputText());
        catalog.register("smoke4/control/input-text", BootstrapPreset.inputText("name", "label", null));
        catalog.register("smoke4/control/input-number", BootstrapBlueprints.inputNumber());
        catalog.register("smoke4/control/radio-group", BootstrapBlueprints.radioGroup());
        catalog.register("smoke4/control/button", BootstrapBlueprints.submitButton());
    }

    private static void registerHtml5Blueprints(BlueprintCatalog catalog) {
        catalog.register("smoke4/form", Html5Blueprints.form());
        catalog.register("smoke4/fragment", CommonBlueprints.fragment());

        catalog.register("smoke4/control/input-text", Html5Blueprints.inputText());
        catalog.register("smoke4/control/input-number", Html5Blueprints.inputNumber());
        catalog.register("smoke4/control/radio-group", Html5Blueprints.radioGroup());
        catalog.register("smoke4/control/button", Html5Blueprints.submitButton());
    }

    // ---------------------------------------------------------------------
    // Common blueprints
    // ---------------------------------------------------------------------

    private static final class CommonBlueprints {

        private CommonBlueprints() {}

        /**
         * Renders {@link FormNodeModel.FragmentModel} by including the referenced blueprint key.
         */
        static Blueprint fragment() {
            return Include.blueprint(
                    Blueprints.path("fragmentKey"),
                    Blueprints.path("model")
            );
        }
    }

    // ---------------------------------------------------------------------
    // Bootstrap theme blueprints
    // ---------------------------------------------------------------------

    private static final class BootstrapBlueprints {

        private BootstrapBlueprints() {}

        static Blueprint form() {
            return Blueprints.element("form", form -> form
                    .attribute("method", Blueprints.constant("POST"))
                    .attribute("class", Blueprints.constant("p-3"))

                    .child(Blueprints.element("h3", h -> h
                            .child(Blueprints.text(Blueprints.path("metadata.title")))
                    ))
                    .child(Blueprints.element("p", p -> p
                            .attribute("class", Blueprints.constant("text-muted"))
                            .child(Blueprints.text(Blueprints.path("metadata.description")))
                    ))

                    .child(Blueprints.repeat(
                            Blueprints.path("content"),
                            "node",
                            body -> body.add(Include.blueprint(
                                    Blueprints.constant("smoke4/fragment"),
                                    Blueprints.path("node")
                            ))
                    ))
            );
        }

        static Blueprint inputText() {
            return Blueprints.element("div", group -> group
                    .attribute("class", Blueprints.constant("mb-3"))

                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .attribute("class", Blueprints.constant("form-label"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))

                    .child(Blueprints.element("input", input -> input
                            .attribute("type", Blueprints.constant("text"))
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                            .attribute("class", Blueprints.constant("form-control"))
                    ))
            );
        }

        static Blueprint inputNumber() {
            return Blueprints.element("div", group -> group
                    .attribute("class", Blueprints.constant("mb-3"))

                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .attribute("class", Blueprints.constant("form-label"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))

                    .child(Blueprints.element("input", input -> input
                            .attribute("type", Blueprints.constant("number"))
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                            .attribute("class", Blueprints.constant("form-control"))
                    ))
            );
        }

        static Blueprint radioGroup() {
            return Blueprints.element("div", group -> group
                    .attribute("class", Blueprints.constant("mb-3"))

                    .child(Blueprints.element("div", title -> title
                            .attribute("class", Blueprints.constant("form-label"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))

                    .child(Blueprints.repeat(
                            Blueprints.path("options"),
                            "option",
                            body -> body.add(Blueprints.element("div", row -> row
                                    .attribute("class", Blueprints.constant("form-check"))

                                    .child(Blueprints.element("input", input -> input
                                            .attribute("type", Blueprints.constant("radio"))
                                            .attribute("class", Blueprints.constant("form-check-input"))
                                            .attribute("name", Blueprints.path("name"))
                                            .attribute("value", Blueprints.path("option.key"))
                                    ))
                                    .child(Blueprints.element("label", label -> label
                                            .attribute("class", Blueprints.constant("form-check-label"))
                                            .child(Blueprints.text(Blueprints.path("option.value")))
                                    ))
                            ))
                    ))
            );
        }

        static Blueprint submitButton() {
            return Blueprints.element("div", block -> block
                    .attribute("class", Blueprints.constant("mt-3"))
                    .child(Blueprints.element("button", button -> button
                            .attribute("type", Blueprints.constant("submit"))
                            .attribute("class", Blueprints.constant("btn btn-primary"))
                            .child(Blueprints.text(Blueprints.path("caption")))
                    ))
            );
        }
    }

    // ---------------------------------------------------------------------
    // HTML5-like theme blueprints (simple, minimal classes)
    // ---------------------------------------------------------------------

    private static final class Html5Blueprints {

        private Html5Blueprints() {}

        static Blueprint form() {
            return Blueprints.element("form", form -> form
                    .attribute("method", Blueprints.constant("POST"))

                    .child(Blueprints.element("h2", h -> h
                            .child(Blueprints.text(Blueprints.path("metadata.title")))
                    ))
                    .child(Blueprints.element("p", p -> p
                            .child(Blueprints.text(Blueprints.path("metadata.description")))
                    ))

                    .child(Blueprints.repeat(
                            Blueprints.path("content"),
                            "node",
                            body -> body.add(Include.blueprint(
                                    Blueprints.constant("smoke4/fragment"),
                                    Blueprints.path("node")
                            ))
                    ))
            );
        }

        static Blueprint inputText() {
            return Blueprints.element("p", block -> block
                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))
                    .child(Blueprints.element("br", br -> {}))
                    .child(Blueprints.element("input", input -> input
                            .attribute("type", Blueprints.constant("text"))
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                    ))
            );
        }

        static Blueprint inputNumber() {
            return Blueprints.element("p", block -> block
                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))
                    .child(Blueprints.element("br", br -> {}))
                    .child(Blueprints.element("input", input -> input
                            .attribute("type", Blueprints.constant("number"))
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                    ))
            );
        }

        static Blueprint radioGroup() {
            return Blueprints.element("fieldset", fieldset -> fieldset
                    .child(Blueprints.element("legend", legend -> legend
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))
                    .child(Blueprints.repeat(
                            Blueprints.path("options"),
                            "option",
                            body -> body.add(Blueprints.element("label", label -> label
                                    .child(Blueprints.element("input", input -> input
                                            .attribute("type", Blueprints.constant("radio"))
                                            .attribute("name", Blueprints.path("name"))
                                            .attribute("value", Blueprints.path("option.key"))
                                    ))
                                    .child(Blueprints.text(Blueprints.constant(" ")))
                                    .child(Blueprints.text(Blueprints.path("option.value")))
                                    .child(Blueprints.element("br", br -> {}))
                            ))
                    ))
            );
        }

        static Blueprint submitButton() {
            return Blueprints.element("p", p -> p
                    .child(Blueprints.element("button", button -> button
                            .attribute("type", Blueprints.constant("submit"))
                            .child(Blueprints.text(Blueprints.path("caption")))
                    ))
            );
        }
    }

    // ---------------------------------------------------------------------
    // Customization hook: checked for radio inputs
    // ---------------------------------------------------------------------

    /**
     * Applies "checked" attribute to radio inputs based on {@link SubmissionState}.
     *
     * <p>This is intentionally separate from {@link SubmissionDecorationHook} to keep
     * responsibilities small and obvious.</p>
     */
    private static final class RadioCheckedHook implements RenderingHook {

        @Override
        public int order() {
            return 1100;
        }

        @Override
        public void afterMaterialize(Node root, RenderingExecution execution) {
            Object submissionRaw = execution.request().attributes().get(SubmissionState.REQUEST_ATTRIBUTE);
            if (!(submissionRaw instanceof SubmissionState submission)) {
                return;
            }

            root.execute(node -> {
                if (!(node instanceof ElementNode element)) {
                    return;
                }
                if (element.getTagName() != TagName.INPUT) {
                    return;
                }

                String type = element.getAttribute("type");
                if (!"radio".equalsIgnoreCase(type)) {
                    return;
                }

                String name = element.getAttribute("name");
                String value = element.getAttribute("value");
                if (name == null || value == null) {
                    return;
                }

                Object submitted = submission.value(name);
                if (submitted != null && value.equals(String.valueOf(submitted))) {
                    element.addAttribute("checked", "checked");
                }
            });
        }
    }

    // ---------------------------------------------------------------------
    // "Database" structure + mapping to stable contract
    // ---------------------------------------------------------------------

    private enum ElementType {
        TEXT, NUMBER, RADIO
    }

    private record DbForm(
            String name,
            String description,
            List<DbField> fields
    ) {}

    private record DbField(
            String name,
            String title,
            ElementType elementType,
            List<DbOption> options
    ) {}

    private record DbOption(
            String key,
            String value
    ) {}

    /**
     * Maps a database-driven form structure into stable blueprint contract models.
     *
     * <p>This mapper intentionally does not depend on client application types.</p>
     */
    private static final class ContractMapper {

        private ContractMapper() {}

        static FormModel toContract(DbForm dbForm) {
            FormMetadata metadata = new FormMetadata(
                    dbForm.name(),
                    dbForm.description(),
                    null,
                    null,
                    Map.of()
            );

            List<FormNodeModel> content = new ArrayList<>();

            for (DbField field : dbForm.fields()) {
                content.add(toFieldNode(field));
            }

            // submit is a regular node
            content.add(submitNode("Submit"));

            return FormModel.of(metadata, List.copyOf(content));
        }

        private static FormNodeModel toFieldNode(DbField field) {
            String blueprintKey = switch (field.elementType()) {
                case TEXT -> "smoke4/control/input-text";
                case NUMBER -> "smoke4/control/input-number";
                case RADIO -> "smoke4/control/radio-group";
            };

            List<OptionModel> options = field.options().stream()
                    .map(o -> new OptionModel(o.key(), o.value()))
                    .toList();

            ControlModel control = new ControlModel(
                    tagNameFor(field.elementType()),
                    semanticsFor(field.elementType()),
                    field.name(),
                    field.title(),
                    null,
                    Map.of(),
                    options,
                    ControlState.empty()
            );

            return new FormNodeModel.FragmentModel(blueprintKey, control);
        }

        private static FormNodeModel submitNode(String caption) {
            // We use ControlModel as the model for submit button blueprint.
            ControlModel submit = new ControlModel(
                    TagName.BUTTON,
                    ControlSemantics.button(),
                    "submit",
                    null,
                    null,
                    Map.of(),
                    List.of(),
                    ControlState.empty()
            );

            // For button blueprint we need "caption" in model.
            // To keep it minimal, we wrap a tiny map-model using FragmentModel.
            Map<String, Object> model = new LinkedHashMap<>();
            model.put("caption", caption);
            model.putAll(Map.of(
                    "tagName", "button",
                    "name", "submit",
                    "label", "null",
                    "attributes", Map.of(),
                    "options", List.of()
            ));

            // Use the same blueprint key for both themes.
            return new FormNodeModel.FragmentModel("smoke4/control/button", model);
        }

        private static TagName tagNameFor(ElementType type) {
            return switch (type) {
                case TEXT, NUMBER -> TagName.INPUT;
                case RADIO -> TagName.DIV;
            };
        }

        private static ControlSemantics semanticsFor(ElementType type) {
            return switch (type) {
                case TEXT -> ControlSemantics.input("text");
                case NUMBER -> ControlSemantics.input("number");
                case RADIO -> ControlSemantics.input("radio");
            };
        }
    }

    // ---------------------------------------------------------------------
    // Small helpers
    // ---------------------------------------------------------------------

    private static RenderingRequest merge(RenderingRequest target, RenderingRequest source) {
        target.attributes().putAll(source.attributes());
        return target;
    }
}
