package org.jmouse.dom.blueprint.smoke;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.dom.Node;
import org.jmouse.dom.blueprint.*;
import org.jmouse.dom.blueprint.dsl.Blueprints;
import org.jmouse.dom.blueprint.dsl.Include;
import org.jmouse.dom.blueprint.hooks.RenderingHook;
import org.jmouse.dom.blueprint.modules.BootstrapThemeModule;
import org.jmouse.dom.blueprint.modules.ThemeAssembly;

import java.util.*;

/**
 * SmokeB shows the core goal:
 * <ul>
 *   <li>client provides a FormDto-like object (here: {@link FormDto})</li>
 *   <li>pipeline builds a node tree using blueprint only</li>
 *   <li>dynamic fields come directly from input (no schema registry, no hardcoded variants)</li>
 *   <li>values and errors are injected from request attributes (data provider style)</li>
 * </ul>
 *
 * <p>Model contract used by blueprints:</p>
 * <ul>
 *   <li>Form model: title, submitLabel, fields</li>
 *   <li>Field model: name, label, blueprintKey, options, value, hasError, errorMessage</li>
 *   <li>Option model: key, value</li>
 * </ul>
 */
public final class SmokeB {

    public static void main(String[] args) {
        AccessorWrapper accessorWrapper = new ObjectAccessorWrapper();

        ThemeAssembly assembly = ThemeAssembly.forTheme(new BootstrapThemeModule())
                .addHook(new FieldStateProviderHook());

        BlueprintCatalog catalog = assembly.catalog();
        registerBlueprints(catalog);

        RenderingPipeline pipeline = assembly.buildPipeline(accessorWrapper);

        // ---------------------------------------------------------------------
        // Client input: FormDto "arrives from request"
        // ---------------------------------------------------------------------

        FormDto formDto = new FormDto(
                "Simple form",
                "Submit",
                List.of(
                        FieldDto.text("username", "Username"),
                        FieldDto.select("country", "Country",
                                        List.of(
                                                new FieldOptionDto("ua", "Ukraine"),
                                                new FieldOptionDto("pl", "Poland"),
                                                new FieldOptionDto("de", "Germany")
                                        )
                        ),
                        FieldDto.textarea("about", "About")
                )
        );

        // "POST" state: client provides posted values and validation errors
        RenderingRequest request = new RenderingRequest()
                .attribute("action", "/submit")
                .attribute("method", "POST")
                .attribute("values", map(
                        "username", "john",
                        "country", "pl",
                        "about", "Hello!"
                ))
                .attribute("errors", map(
                        "username", "Username is too short"
                ));

        Node node = pipeline.render("smokeB/form", formDto, r -> merge(r, request));

        // System.out.println(node.interpret(new NodeContext()));
    }

    private static void registerBlueprints(BlueprintCatalog catalog) {
        Verify.nonNull(catalog, "catalog");

        catalog.register("smokeB/form", SmokeBlueprints.form());
        catalog.register("smokeB/field", SmokeBlueprints.fieldDelegator());

        catalog.register("smokeB/field/text", SmokeBlueprints.textInput("text"));
        catalog.register("smokeB/field/textarea", SmokeBlueprints.textarea());
        catalog.register("smokeB/field/select", SmokeBlueprints.select());

        catalog.register("smokeB/feedback/invalid", SmokeBlueprints.invalidFeedback());
        catalog.register("smokeB/button/submit", SmokeBlueprints.submitButton());
    }

    // -------------------------------------------------------------------------
    // Blueprints
    // -------------------------------------------------------------------------

    private static final class SmokeBlueprints {

        private SmokeBlueprints() {}

        static Blueprint form() {
            return Blueprints.element("form", form -> form
                    .attribute("method", Blueprints.requestAttribute("method"))
                    .attribute("action", Blueprints.requestAttribute("action"))

                    .child(Blueprints.element("h3", h -> h
                            .child(Blueprints.text(Blueprints.path("title")))
                    ))

                    .child(Blueprints.repeat(
                            Blueprints.path("fields"),
                            "field",
                            body -> body.add(Include.blueprint(
                                    Blueprints.path("field.blueprintKey"),
                                    Blueprints.path("field")
                            ))
                    ))

                    .child(Include.blueprint(
                            Blueprints.constant("smokeB/button/submit"),
                            Blueprints.path("")
                    ))
            );
        }

        static Blueprint fieldDelegator() {
            return Include.blueprint(
                    Blueprints.path("blueprintKey"),
                    Blueprints.path("")
            );
        }

        static Blueprint textInput(String type) {
            return Blueprints.element("div", block -> block
                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))
                    .child(Blueprints.element("input", input -> input
                            .attribute("type", Blueprints.constant(type))
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                            .attribute("value", Blueprints.path("value"))
                    ))
                    .child(Blueprints.when(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smokeB/feedback/invalid"),
                                    Blueprints.path("")
                            ))
                    ))
            );
        }

        static Blueprint textarea() {
            return Blueprints.element("div", block -> block
                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))
                    .child(Blueprints.element("textarea", area -> area
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                            .child(Blueprints.text(Blueprints.path("value")))
                    ))
                    .child(Blueprints.when(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smokeB/feedback/invalid"),
                                    Blueprints.path("")
                            ))
                    ))
            );
        }

        static Blueprint select() {
            return Blueprints.element("div", block -> block
                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))
                    .child(Blueprints.element("select", select -> select
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                            .child(Blueprints.repeat(
                                    Blueprints.path("options"),
                                    "option",
                                    body -> body.add(Blueprints.element("option", option -> option
                                            .attribute("value", Blueprints.path("option.key"))
                                            .child(Blueprints.text(Blueprints.path("option.value")))
                                    ))
                            ))
                    ))
                    .child(Blueprints.when(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smokeB/feedback/invalid"),
                                    Blueprints.path("")
                            ))
                    ))
            );
        }

        static Blueprint submitButton() {
            return Blueprints.element("div", block -> block
                    .child(Blueprints.element("button", button -> button
                            .attribute("type", Blueprints.constant("submit"))
                            .child(Blueprints.text(Blueprints.path("submitLabel")))
                    ))
            );
        }

        static Blueprint invalidFeedback() {
            return Blueprints.element("div", div -> div
                    .attribute("class", Blueprints.constant("invalid-feedback"))
                    .child(Blueprints.text(Blueprints.path("errorMessage")))
            );
        }
    }

    // -------------------------------------------------------------------------
    // Hook: data provider style (values + errors) + dynamic blueprintKey resolution
    // -------------------------------------------------------------------------

    /**
     * Injects posted values and errors into field models.
     *
     * <p>Also resolves field blueprint keys from ElementType, so client input can be minimal.</p>
     */
    private static final class FieldStateProviderHook implements RenderingHook {

        @Override
        public int order() {
            return 1000;
        }

        @Override
        public void beforeBlueprintResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
            if (!(data instanceof FormDto formDto)) {
                return;
            }

            Map<String, Object> values = asMap(request.attributes().get("values"));
            Map<String, Object> errors = asMap(request.attributes().get("errors"));

            // Mutate fields in-place for simplicity in smoke.
            for (FieldDto field : formDto.fields()) {
                String name = field.name();

                if (values.containsKey(name)) {
                    field.value = values.get(name);
                }

                if (errors.containsKey(name)) {
                    field.hasError = true;
                    field.errorMessage = String.valueOf(errors.get(name));
                } else {
                    field.hasError = false;
                    field.errorMessage = null;
                }

                // Resolve blueprintKey from element type
                field.blueprintKey = resolveFieldBlueprintKey(field.elementType());
            }
        }

        private String resolveFieldBlueprintKey(ElementType elementType) {
            return switch (elementType) {
                case TEXT -> "smokeB/field/text";
                case TEXTAREA -> "smokeB/field/textarea";
                case SELECT -> "smokeB/field/select";
            };
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> asMap(Object value) {
            if (value == null) return new LinkedHashMap<>();
            if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
            throw new IllegalStateException("Expected Map but got: " + value.getClass().getName());
        }
    }

    // -------------------------------------------------------------------------
    // Client input model (simulates request.getFormDto())
    // -------------------------------------------------------------------------

    /**
     * Client-provided form model that "arrives from request".
     *
     * <p>This model intentionally looks like a typical DTO:
     * title, submit label, and list of fields.</p>
     */
    public record FormDto(String title, String submitLabel, List<FieldDto> fields) {}

    /**
     * Field model resembling FieldDto.
     *
     * <p>To keep this smoke minimal, some properties are mutable to allow hook injection
     * (value, error flags, blueprintKey).</p>
     */
    public static final class FieldDto {

        private final String elementTypeTagName;
        private final String name;
        private final String label;
        private final List<FieldOptionDto> options;

        private Object value;
        private boolean hasError;
        private String errorMessage;
        private String blueprintKey;

        private FieldDto(String elementTypeTagName, String name, String label, List<FieldOptionDto> options) {
            this.elementTypeTagName = elementTypeTagName;
            this.name = name;
            this.label = label;
            this.options = options;
        }

        public static FieldDto text(String name, String label) {
            return new FieldDto("input", name, label, List.of());
        }

        public static FieldDto textarea(String name, String label) {
            return new FieldDto("textarea", name, label, List.of());
        }

        public static FieldDto select(String name, String label, List<FieldOptionDto> options) {
            return new FieldDto("select", name, label, options);
        }

        public ElementType elementType() {
            return switch (elementTypeTagName) {
                case "input" -> ElementType.TEXT;
                case "textarea" -> ElementType.TEXTAREA;
                case "select" -> ElementType.SELECT;
                default -> throw new IllegalStateException("Unsupported element tag: " + elementTypeTagName);
            };
        }

        public String getElementTypeTagName() {
            return elementTypeTagName;
        }

        public String name() {
            return name;
        }

        public String label() {
            return label;
        }

        public List<FieldOptionDto> options() {
            return options;
        }

        // The following methods provide "blueprint contract" paths:
        // name, label, value, options, hasError, errorMessage, blueprintKey

        public Object value() {
            return value;
        }

        public boolean hasError() {
            return hasError;
        }

        public String errorMessage() {
            return errorMessage;
        }

        public String blueprintKey() {
            return blueprintKey;
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public List<FieldOptionDto> getOptions() {
            return options;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public boolean isHasError() {
            return hasError;
        }

        public void setHasError(boolean hasError) {
            this.hasError = hasError;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getBlueprintKey() {
            return blueprintKey;
        }

        public void setBlueprintKey(String blueprintKey) {
            this.blueprintKey = blueprintKey;
        }
    }

    public record FieldOptionDto(String key, String value) {}

    /**
     * Minimal element types for this smoke.
     */
    public enum ElementType {
        TEXT, TEXTAREA, SELECT;

        public boolean isOptionable() {
            return this == SELECT;
        }

        public String getTagName() {
            return switch (this) {
                case TEXT -> "input";
                case TEXTAREA -> "textarea";
                case SELECT -> "select";
            };
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static RenderingRequest merge(RenderingRequest target, RenderingRequest source) {
        target.attributes().putAll(source.attributes());
        return target;
    }

    private static Map<String, Object> map(Object... keyValues) {
        Verify.nonNull(keyValues, "keyValues");
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key-values must be even.");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
