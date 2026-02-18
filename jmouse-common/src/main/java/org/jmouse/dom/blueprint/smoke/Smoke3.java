package org.jmouse.dom.blueprint.smoke;

import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.TagName;
import org.jmouse.dom.blueprint.*;
import org.jmouse.dom.blueprint.build.Blueprints;
import org.jmouse.dom.blueprint.build.Include;
import org.jmouse.dom.blueprint.hooks.SubmissionDecorationHook;
import org.jmouse.dom.blueprint.model.*;
import org.jmouse.dom.blueprint.modules.BootstrapThemeModule;
import org.jmouse.dom.blueprint.modules.ThemeAssembly;

import java.util.List;
import java.util.Map;

/**
 * Smoke3:
 * <ul>
 *   <li>builds a {@link FormModel} using stable contract</li>
 *   <li>renders using blueprint catalog</li>
 *   <li>applies submitted values and errors via {@link SubmissionDecorationHook}</li>
 * </ul>
 */
public final class Smoke3 {

    public static void main(String[] args) {
        ThemeAssembly assembly = ThemeAssembly.forTheme(new BootstrapThemeModule())
                .addHook(new SubmissionDecorationHook());

        BlueprintCatalog catalog = assembly.catalog();
        registerBlueprints(catalog);

        RenderingPipeline pipeline = assembly.build(new ObjectAccessorWrapper());

        FormModel form = buildForm();

        RenderingRequest request = new RenderingRequest()
                .attribute(SubmissionState.REQUEST_ATTRIBUTE,
                           SubmissionState.of(
                                   Map.of("email", "john@example.com", "country", "pl"),
                                   Map.of("password", "Password is required")
                           )
                );

        Node node = pipeline.render("smoke3/form", form, r -> merge(r, request));
        node.execute(NodeContext.REORDER_NODE_CORRECTOR);
        System.out.println(node.interpret(NodeContext.defaults()));
    }

    private static RenderingRequest merge(RenderingRequest target, RenderingRequest source) {
        target.attributes().putAll(source.attributes());
        return target;
    }

    private static void registerBlueprints(BlueprintCatalog catalog) {
        catalog.register("smoke3/form", Smoke3Blueprints.form());
        catalog.register("smoke3/fragment", Smoke3Blueprints.fragment());

        catalog.register("smoke3/control/input", Smoke3Blueprints.inputControl());
        catalog.register("smoke3/control/select", Smoke3Blueprints.selectControl());
        catalog.register("smoke3/control/button", Smoke3Blueprints.buttonControl());
    }

    private static FormModel buildForm() {
        FormMetadata metadata = new FormMetadata(
                "User registration",
                "Smoke form built from contract",
                "/submit",
                "POST",
                Map.of("class", "p-3")
        );

        ControlModel email = new ControlModel(
                TagName.INPUT,
                ControlSemantics.input("email"),
                "email",
                "Email",
                null,
                Map.of("class", "form-control", "placeholder", "Email", "type", "email"),
                List.of(),
                ControlState.empty()
        );

        ControlModel password = new ControlModel(
                TagName.INPUT,
                ControlSemantics.input("password"),
                "password",
                "Password",
                null,
                Map.of("class", "form-control", "placeholder", "Password", "type", "password"),
                List.of(),
                ControlState.empty()
        );

        ControlModel country = new ControlModel(
                TagName.SELECT,
                ControlSemantics.select(),
                "country",
                "Country",
                null,
                Map.of("class", "form-select"),
                List.of(
                        new OptionModel("ua", "Ukraine"),
                        new OptionModel("pl", "Poland"),
                        new OptionModel("de", "Germany")
                ),
                ControlState.empty()
        );

        ControlModel submit = new ControlModel(
                TagName.BUTTON,
                ControlSemantics.button(),
                "submit",
                null,
                null,
                Map.of("type", "submit", "class", "btn btn-primary", "value", "Submit"),
                List.of(),
                ControlState.empty()
        );

        // Data-level include: FragmentModel(fragmentKey, model)
        FormNodeModel.FragmentModel emailNode = new FormNodeModel.FragmentModel("smoke3/control/input", email);
        FormNodeModel.FragmentModel passwordNode = new FormNodeModel.FragmentModel("smoke3/control/input", password);
        FormNodeModel.FragmentModel countryNode = new FormNodeModel.FragmentModel("smoke3/control/select", country);
        FormNodeModel.FragmentModel submitNode = new FormNodeModel.FragmentModel("smoke3/control/button", submit);

        return FormModel.of(metadata, List.of(emailNode, passwordNode, countryNode, submitNode));
    }

    // -------------------------------------------------------------------------
    // Blueprints
    // -------------------------------------------------------------------------

    private static final class Smoke3Blueprints {

        private Smoke3Blueprints() {}

        static Blueprint form() {
            return Blueprints.element("form", form -> form
                    .attribute("action", Blueprints.path("metadata.action"))
                    .attribute("method", Blueprints.path("metadata.method"))
                    .attribute("class", Blueprints.path("metadata.attributes.class"))

                    .child(Blueprints.element("h3", h -> h
                            .child(Blueprints.text(Blueprints.path("metadata.title")))
                    ))

                    .child(Blueprints.repeat(
                            Blueprints.path("content"),
                            "node",
                            body -> body.add(Include.blueprint(
                                    Blueprints.constant("smoke3/fragment"),
                                    Blueprints.path("node")
                            ))
                    ))
            );
        }

        /**
         * Renders {@link FormNodeModel.FragmentModel} by including the target blueprint.
         */
        static Blueprint fragment() {
            return Include.blueprint(
                    Blueprints.path("fragmentKey"),
                    Blueprints.path("model")
            );
        }

        static Blueprint inputControl() {
            return Blueprints.element("div", wrapper -> wrapper
                    .attribute("class", Blueprints.constant("mb-3"))

                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .attribute("class", Blueprints.constant("form-label"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))

                    .child(Blueprints.element("input", input -> input
                            .attribute("type", Blueprints.path("attributes.type"))
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                            .attribute("class", Blueprints.path("attributes.class"))
                            .attribute("placeholder", Blueprints.path("attributes.placeholder"))
                    ))
            );
        }

        static Blueprint selectControl() {
            return Blueprints.element("div", wrapper -> wrapper
                    .attribute("class", Blueprints.constant("mb-3"))

                    .child(Blueprints.element("label", label -> label
                            .attribute("for", Blueprints.path("name"))
                            .attribute("class", Blueprints.constant("form-label"))
                            .child(Blueprints.text(Blueprints.path("label")))
                    ))

                    .child(Blueprints.element("select", select -> select
                            .attribute("name", Blueprints.path("name"))
                            .attribute("id", Blueprints.path("name"))
                            .attribute("class", Blueprints.path("attributes.class"))

                            .child(Blueprints.repeat(
                                    Blueprints.path("options"),
                                    "option",
                                    body -> body.add(Blueprints.element("option", option -> option
                                            .attribute("value", Blueprints.path("option.key"))
                                            .child(Blueprints.text(Blueprints.path("option.value")))
                                    ))
                            ))
                    ))
            );
        }

        static Blueprint buttonControl() {
            return Blueprints.element("div", wrapper -> wrapper
                    .attribute("class", Blueprints.constant("mt-3"))
                    .child(Blueprints.element("button", button -> button
                            .attribute("type", Blueprints.path("attributes.type"))
                            .attribute("class", Blueprints.path("attributes.class"))
                            .child(Blueprints.text(Blueprints.path("attributes.value")))
                    ))
            );
        }
    }
}
