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
import org.jmouse.dom.blueprint.transform.ContextMatch;
import org.jmouse.dom.blueprint.transform.ContextRuleBasedBlueprintTransformer;
import org.jmouse.dom.blueprint.transform.Change;

import java.util.*;

/**
 * Smoke demo that shows:
 * <ul>
 *   <li>Blueprint catalog + theme assembly</li>
 *   <li>Data provider-like behavior via request attributes (values + errors)</li>
 *   <li>Dynamic form generation driven by input without hardcoded if/else blocks</li>
 *   <li>Transformer override (Bootstrap-like styling)</li>
 *   <li>Rendering of: authorization, registration, and dynamic form</li>
 * </ul>
 *
 * <p>This class is intentionally self-contained to demonstrate the full mechanism in one place.</p>
 */
public final class SmokeA {

    public static void main(String[] args) {
        AccessorWrapper accessorWrapper = new ObjectAccessorWrapper();

        // 1) Build theme assembly (Bootstrap) + overrides
        ThemeAssembly assembly = ThemeAssembly.forTheme(new BootstrapThemeModule())
                .addHook(new FieldStateProviderHook())
                .addTransformer(1000, ContextRuleBasedBlueprintTransformer.builder()
                        .rule(1000, ContextMatch.tagName("form"), Change.addClass("p-3"))
                        .rule(1000, ContextMatch.tagName("button"), Change.addClass("btn btn-primary"))
                        .build()
                );

        // 2) Register smoke blueprints (form + field + button + feedback)
        BlueprintCatalog catalog = assembly.catalog();
        registerSmokeBlueprints(catalog);

        // 3) Create pipeline
        RenderingPipeline pipeline = assembly.buildPipeline(accessorWrapper);

        // --- AUTHORIZATION ---
        Map<String, Object> loginModel = model(
                "title", "Sign in",
                "schema", "auth/login",
                "submitLabel", "Sign in"
        );

        RenderingRequest loginRequest = new RenderingRequest()
                .attribute("action", "/auth/login")
                .attribute("method", "POST")
                // submitted values
                .attribute("values", model("email", "john@example.com", "password", ""))
                // field errors
                .attribute("errors", model("password", "Password is required"));

        Node loginNode = pipeline.render("smoke/form", loginModel, r -> merge(r, loginRequest));
        System.out.println("=== AUTHORIZATION ===");
        
//        System.out.println(loginNode.interpret(new NodeContext()));

        // --- REGISTRATION ---
        Map<String, Object> registrationModel = model(
                "title", "Create account",
                "schema", "auth/register",
                "submitLabel", "Create account"
        );

        RenderingRequest registrationRequest = new RenderingRequest()
                .attribute("action", "/auth/register")
                .attribute("method", "POST")
                .attribute("values", model(
                        "email", "jane@example.com",
                        "username", "jane",
                        "password", "123",
                        "confirmPassword", "12"
                ))
                .attribute("errors", model(
                        "confirmPassword", "Passwords do not match"
                ));

        Node registrationNode = pipeline.render("smoke/form", registrationModel, r -> merge(r, registrationRequest));
        System.out.println("=== REGISTRATION ===");
        //System.out.println(registrationNode.interpret(new NodeContext()));

        // --- DYNAMIC FORM (depends on input) ---
        // Initial request: user selects profile type
        Map<String, Object> dynamicModel = model(
                "title", "Dynamic profile",
                "schema", "profile/dynamic",
                "submitLabel", "Continue"
        );

        RenderingRequest dynamicInitialRequest = new RenderingRequest()
                .attribute("action", "/profile/dynamic")
                .attribute("method", "POST")
                .attribute("values", model("profileType", "company")) // user selected company
                .attribute("errors", model());

        Node dynamicInitialNode = pipeline.render("smoke/form", dynamicModel, r -> merge(r, dynamicInitialRequest));
        System.out.println("=== DYNAMIC FORM (after selecting company) ===");
//        System.out.println(dynamicInitialNode.interpret(new NodeContext()));

        // Next request: user filled company form
        RenderingRequest dynamicCompanyRequest = new RenderingRequest()
                .attribute("action", "/profile/dynamic")
                .attribute("method", "POST")
                .attribute("values", model(
                        "profileType", "company",
                        "companyName", "ACME",
                        "taxId", "12",
                        "employees", "0"
                ))
                .attribute("errors", model(
                        "taxId", "Tax ID must be 8-12 digits",
                        "employees", "Employees must be at least 1"
                ));

        Node dynamicCompanyNode = pipeline.render("smoke/form", dynamicModel, r -> merge(r, dynamicCompanyRequest));
        System.out.println("=== DYNAMIC FORM (company with errors) ===");
//        System.out.println(dynamicCompanyNode.interpret(new NodeContext()));
    }

    /**
     * Registers smoke-level blueprints:
     * <ul>
     *   <li>smoke/form - renders a form by schema</li>
     *   <li>smoke/field/* - field types</li>
     *   <li>smoke/button/submit</li>
     *   <li>smoke/feedback/invalid</li>
     * </ul>
     */
    private static void registerSmokeBlueprints(BlueprintCatalog catalog) {
        Verify.nonNull(catalog, "catalog");

        catalog.register("smoke/form", SmokeBlueprints.form());
        catalog.register("smoke/field", SmokeBlueprints.fieldDelegator());

        catalog.register("smoke/field/text", SmokeBlueprints.textInput("text"));
        catalog.register("smoke/field/email", SmokeBlueprints.textInput("email"));
        catalog.register("smoke/field/password", SmokeBlueprints.textInput("password"));
        catalog.register("smoke/field/date", SmokeBlueprints.textInput("date"));
        catalog.register("smoke/field/number", SmokeBlueprints.textInput("number"));

        catalog.register("smoke/field/textarea", SmokeBlueprints.textarea());
        catalog.register("smoke/field/select", SmokeBlueprints.select());
        catalog.register("smoke/field/checkbox", SmokeBlueprints.checkbox());
        catalog.register("smoke/field/radio", SmokeBlueprints.radioGroup());

        catalog.register("smoke/button/submit", SmokeBlueprints.submitButton());
        catalog.register("smoke/feedback/invalid", SmokeBlueprints.invalidFeedback());
    }

    // -------------------------------------------------------------------------
    // Blueprint definitions
    // -------------------------------------------------------------------------

    private static final class SmokeBlueprints {

        private SmokeBlueprints() {}

        /**
         * Form blueprint:
         * - selects schema by key: model.schema
         * - generates field list from schema registry (SchemaRegistry)
         * - uses request attributes "values" and "errors" via FieldStateProviderHook
         */
        static Blueprint form() {
            return Blueprints.element("form", form -> form
                    .attribute("method", Blueprints.requestAttribute("method"))
                    .attribute("action", Blueprints.requestAttribute("action"))

                    .child(Blueprints.element("h3", h -> h
                            .child(Blueprints.text(Blueprints.path("title")))
                    ))

                    // repeat over computed fields from schema registry
                    .child(Blueprints.repeat(
                            Blueprints.path("fields"),
                            "field",
                            body -> body.add(Include.blueprint(
                                    Blueprints.path("field.blueprintKey"),
                                    Blueprints.path("field")
                            ))
                    ))

                    .child(Include.blueprint(
                            Blueprints.constant("smoke/button/submit"),
                            Blueprints.path("")
                    ))
            );
        }

        /**
         * Delegates rendering to field.blueprintKey.
         */
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
                            .attribute("placeholder", Blueprints.path("label"))
                    ))
                    .child(Blueprints.conditional(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smoke/feedback/invalid"),
                                    Blueprints.path("")
                            )),
                            whenFalse -> whenFalse
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
                    .child(Blueprints.conditional(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smoke/feedback/invalid"),
                                    Blueprints.path("")
                            )),
                            whenFalse -> whenFalse
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
                    .child(Blueprints.conditional(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smoke/feedback/invalid"),
                                    Blueprints.path("")
                            )),
                            whenFalse -> whenFalse
                    ))
            );
        }

        static Blueprint checkbox() {
            return Blueprints.element("div", block -> block
                    .child(Blueprints.element("div", wrapper -> wrapper
                            .child(Blueprints.element("input", input -> input
                                    .attribute("type", Blueprints.constant("checkbox"))
                                    .attribute("name", Blueprints.path("name"))
                                    .attribute("id", Blueprints.path("name"))
                                    .attribute("value", Blueprints.path("value"))
                            ))
                            .child(Blueprints.element("label", label -> label
                                    .attribute("for", Blueprints.path("name"))
                                    .child(Blueprints.text(Blueprints.path("label")))
                            ))
                    ))
                    .child(Blueprints.conditional(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smoke/feedback/invalid"),
                                    Blueprints.path("")
                            )),
                            whenFalse -> whenFalse
                    ))
            );
        }

        static Blueprint radioGroup() {
            return Blueprints.element("div", block -> block
                    .child(Blueprints.element("label", label -> label
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))
                    .child(Blueprints.repeat(
                            Blueprints.path("options"),
                            "option",
                            body -> body.add(Blueprints.element("div", wrapper -> wrapper
                                    .child(Blueprints.element("input", input -> input
                                            .attribute("type", Blueprints.constant("radio"))
                                            .attribute("name", Blueprints.path("name"))
                                            .attribute("value", Blueprints.path("option.key"))
                                    ))
                                    .child(Blueprints.element("label", label -> label
                                            .child(Blueprints.text(Blueprints.path("option.value")))
                                    ))
                            ))
                    ))
                    .child(Blueprints.conditional(
                            Blueprints.pathBoolean("hasError"),
                            whenTrue -> whenTrue.add(Include.blueprint(
                                    Blueprints.constant("smoke/feedback/invalid"),
                                    Blueprints.path("")
                            )),
                            whenFalse -> whenFalse
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
    // Data provider-like behavior
    // -------------------------------------------------------------------------

    /**
     * Emulates legacy data provider behavior:
     * - fills field.value from request attribute "values" (Map)
     * - fills field.hasError and field.errorMessage from request attribute "errors" (Map)
     * - computes model.fields from schema + input (dynamic forms)
     *
     * <p>This hook demonstrates how client code can supply posted values and errors without changing DTO structures.</p>
     */
    private static final class FieldStateProviderHook implements RenderingHook {

        @Override
        public int order() {
            return 1000;
        }

        @Override
        public void beforeBlueprintResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
            // Build dynamic fields from schema without if/else:
            // schemaKey -> list of field descriptors
            Map<String, Object> model = asMap(data);
            String schemaKey = String.valueOf(model.get("schema"));

            Map<String, Object> values = asMap(request.attributes().getOrDefault("values", model()));
            Map<String, Object> errors = asMap(request.attributes().getOrDefault("errors", model()));

            List<Map<String, Object>> fields = SchemaRegistry.buildFields(schemaKey, values);

            // Apply values + errors into each field descriptor
            for (Map<String, Object> field : fields) {
                String name = String.valueOf(field.get("name"));

                Object postedValue = values.get(name);
                if (postedValue != null) {
                    field.put("value", postedValue);
                }

                Object error = errors.get(name);
                if (error != null) {
                    field.put("hasError", true);
                    field.put("errorMessage", String.valueOf(error));
                } else {
                    field.put("hasError", false);
                    field.put("errorMessage", null);
                }
            }

            model.put("fields", fields);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> asMap(Object value) {
            if (value == null) return new LinkedHashMap<>();
            if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
            throw new IllegalStateException("Expected Map but got: " + value.getClass().getName());
        }
    }

    /**
     * Schema registry that defines forms as data (no hardcoded if/else in the generator).
     *
     * <p>Dynamic behavior is implemented as a schema rule that reads input values.</p>
     */
    private static final class SchemaRegistry {

        private static final Map<String, Schema> SCHEMAS = new LinkedHashMap<>();

        static {
            // Authorization schema
            register("auth/login", Schema.builder()
                    .field(FieldDescriptor.text("email", "Email").blueprintKey("smoke/field/email"))
                    .field(FieldDescriptor.text("password", "Password").blueprintKey("smoke/field/password"))
                    .field(FieldDescriptor.checkbox("rememberMe", "Remember me").blueprintKey("smoke/field/checkbox"))
                    .build()
            );

            // Registration schema
            register("auth/register", Schema.builder()
                    .field(FieldDescriptor.text("email", "Email").blueprintKey("smoke/field/email"))
                    .field(FieldDescriptor.text("username", "Username").blueprintKey("smoke/field/text"))
                    .field(FieldDescriptor.text("password", "Password").blueprintKey("smoke/field/password"))
                    .field(FieldDescriptor.text("confirmPassword", "Confirm password").blueprintKey("smoke/field/password"))
                    .field(FieldDescriptor.select("country", "Country",
                                                  List.of(
                                                          option("ua", "Ukraine"),
                                                          option("pl", "Poland"),
                                                          option("de", "Germany")
                                                  )).blueprintKey("smoke/field/select")
                    )
                    .field(FieldDescriptor.checkbox("agreeTerms", "I agree to the Terms").blueprintKey("smoke/field/checkbox"))
                    .build()
            );

            // Dynamic schema: profile/dynamic
            register("profile/dynamic", Schema.builder()
                    .field(FieldDescriptor.select("profileType", "Profile type",
                                                  List.of(
                                                          option("individual", "Individual"),
                                                          option("company", "Company")
                                                  )).blueprintKey("smoke/field/select")
                    )
                    .dynamic(new ProfileTypeDynamicRule())
                    .build()
            );
        }

        static List<Map<String, Object>> buildFields(String schemaKey, Map<String, Object> values) {
            Schema schema = SCHEMAS.get(schemaKey);
            if (schema == null) {
                return List.of();
            }
            return schema.materialize(values);
        }

        static void register(String key, Schema schema) {
            SCHEMAS.put(key, schema);
        }

        private static Map<String, Object> option(String key, String value) {
            return model("key", key, "value", value);
        }
    }

    private interface DynamicRule {
        List<FieldDescriptor> resolve(Map<String, Object> values);
    }

    private static final class ProfileTypeDynamicRule implements DynamicRule {

        @Override
        public List<FieldDescriptor> resolve(Map<String, Object> values) {
            String mode = String.valueOf(values.getOrDefault("profileType", "none"));

            Map<String, List<FieldDescriptor>> variants = new LinkedHashMap<>();
            variants.put("individual", List.of(
                    FieldDescriptor.text("firstName", "First name").blueprintKey("smoke/field/text"),
                    FieldDescriptor.text("lastName", "Last name").blueprintKey("smoke/field/text"),
                    FieldDescriptor.text("birthDate", "Birth date").blueprintKey("smoke/field/date")
            ));
            variants.put("company", List.of(
                    FieldDescriptor.text("companyName", "Company name").blueprintKey("smoke/field/text"),
                    FieldDescriptor.text("taxId", "Tax ID").blueprintKey("smoke/field/text"),
                    FieldDescriptor.text("employees", "Employees").blueprintKey("smoke/field/number")
            ));

            return variants.getOrDefault(mode, List.of());
        }
    }

    private static final class Schema {

        private final List<FieldDescriptor> baseFields;
        private final List<DynamicRule> dynamicRules;

        private Schema(List<FieldDescriptor> baseFields, List<DynamicRule> dynamicRules) {
            this.baseFields = baseFields;
            this.dynamicRules = dynamicRules;
        }

        List<Map<String, Object>> materialize(Map<String, Object> values) {
            List<FieldDescriptor> all = new ArrayList<>(baseFields);

            for (DynamicRule rule : dynamicRules) {
                all.addAll(rule.resolve(values));
            }

            List<Map<String, Object>> result = new ArrayList<>(all.size());
            for (FieldDescriptor d : all) {
                result.add(d.toModel());
            }
            return List.copyOf(result);
        }

        static Builder builder() {
            return new Builder();
        }

        private static final class Builder {
            private final List<FieldDescriptor> fields = new ArrayList<>();
            private final List<DynamicRule> dynamics = new ArrayList<>();

            Builder field(FieldDescriptor d) {
                fields.add(d);
                return this;
            }

            Builder dynamic(DynamicRule rule) {
                dynamics.add(rule);
                return this;
            }

            Schema build() {
                return new Schema(List.copyOf(fields), List.copyOf(dynamics));
            }
        }
    }

    private static final class FieldDescriptor {

        private final Map<String, Object> model = new LinkedHashMap<>();

        private FieldDescriptor(String name, String label) {
            model.put("name", name);
            model.put("label", label);
            model.put("value", null);
            model.put("options", List.of());
            model.put("hasError", false);
            model.put("errorMessage", null);
        }

        static FieldDescriptor text(String name, String label) {
            FieldDescriptor d = new FieldDescriptor(name, label);
            d.model.put("blueprintKey", "smoke/field/text");
            return d;
        }

        static FieldDescriptor checkbox(String name, String label) {
            FieldDescriptor d = new FieldDescriptor(name, label);
            d.model.put("blueprintKey", "smoke/field/checkbox");
            return d;
        }

        static FieldDescriptor select(String name, String label, List<Map<String, Object>> options) {
            FieldDescriptor d = new FieldDescriptor(name, label);
            d.model.put("options", options);
            d.model.put("blueprintKey", "smoke/field/select");
            return d;
        }

        FieldDescriptor blueprintKey(String key) {
            model.put("blueprintKey", key);
            return this;
        }

        Map<String, Object> toModel() {
            return new LinkedHashMap<>(model);
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private static RenderingRequest merge(RenderingRequest target, RenderingRequest source) {
        target.attributes().putAll(source.attributes());
        return target;
    }

    private static Map<String, Object> model(Object... keyValues) {
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
