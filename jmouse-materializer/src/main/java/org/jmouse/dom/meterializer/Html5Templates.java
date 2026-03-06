package org.jmouse.dom.meterializer;

import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.meterializer.ValueExpression;
import org.jmouse.util.Strings;

import static org.jmouse.meterializer.NodeTemplate.*;
import static org.jmouse.meterializer.TemplatePredicate.present;
import static org.jmouse.meterializer.ValueExpression.*;

public final class Html5Templates {

    private Html5Templates() {}

    public static NodeTemplate button(String type, String defaultCaption) {
        return element("p", block -> block
                .child(element("button", button -> button
                        .attribute("type", constant(type))
                        .attribute("class", constant("btn btn-sm btn-dark"))
                        .child(when(
                                present(request("submitCaption")),
                                t -> t.add(text(request("submitCaption"))),
                                f -> f.add(when(
                                        present(path("description")),
                                        t -> t.add(text(constant("Submit: "))).add(text(path("description"))),
                                        k -> k.add(text(constant(defaultCaption)))
                                ))
                        ))
                ))
        );
    }

    public static NodeTemplate submitButton(String caption) {
        return button("submit", caption);
    }

    public static NodeTemplate inputButton(String caption) {
        return button("button", caption);
    }

    public static NodeTemplate input(InputType type, String namePath, String labelPath, String valuePath) {
        return element("div", block -> block
                .child(element("label", l -> l
                        .attribute("for", path("id"))
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(element("input", b -> b
                        .attribute("type", constant(type.htmlValue()))
                        .attribute("id", path("id"))
                        .attribute("name", path(namePath))
                        .attribute("class", constant("form-control"))
                        .attributeIf(present(constant(valuePath)), "value", request(valuePath))
                        .attributes(path("attributes"), "name", "value")
                ))
        );
    }

    public static NodeTemplate inputText(String namePath, String labelPath, String valuePath) {
        return input(InputType.TEXT, namePath, labelPath, valuePath);
    }

    public static NodeTemplate inputNumber(String namePath, String labelPath, String valuePath) {
        return input(InputType.NUMBER, namePath, labelPath, valuePath);
    }

    public static NodeTemplate inputEmail(String namePath, String labelPath, String valuePath) {
        return input(InputType.EMAIL, namePath, labelPath, valuePath);
    }

    public static NodeTemplate inputPassword(String namePath, String labelPath, String valuePath) {
        return input(InputType.PASSWORD, namePath, labelPath, valuePath);
    }

    public static NodeTemplate select(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", block -> block
                .child(element("label", label -> label
                        .attribute("for", path("id"))
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(element("select", select -> select
                        .attributes(path("attributes"), "name", "value")
                        .attribute("id", path("id"))
                        .attribute("name", path(namePath))
                        .attribute("class", constant("form-select"))
                        .child(repeat(
                                path(optionsPath),
                                "option",
                                inner -> inner.add(
                                        element("option", option -> option
                                                .attribute("value", path(keyPath))
                                                .child(text(path(valuePath)))
                                        )
                                ),
                                ""
                        ))
                ))
        );
    }

    public static NodeTemplate checkboxSingle(String namePath, String labelPath, String valuePath) {
        return element("p", block -> block
                .child(element("label", label -> {
                    label.child(element("input", input -> {
                        input.attribute("type", constant("checkbox"));
                        input.attribute("name", path(namePath));
                        input.attribute("id", path(namePath));

                        if (Strings.isNotEmpty(valuePath)) {
                            input.attribute("value", path(valuePath));
                        }
                    }));
                    label.child(text(constant(" ")));
                    label.child(text(path(labelPath)));
                }))
        );
    }

    public static NodeTemplate radioGroup(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("fieldset", fieldset -> fieldset
                .child(element("legend", legend -> legend
                        .child(text(path(labelPath)))
                ))
                .child(repeat(
                        path(optionsPath),
                        "option",
                        inner -> inner.add(element("label", label -> {
                            label.child(element("input", input -> input
                                    .attribute("type", constant("radio"))
                                    .attribute("name", path(namePath))
                                    .attribute("value", path(keyPath))
                            ));
                            label.child(text(constant(" ")));
                            label.child(text(path(valuePath)));
                            label.child(element("br", br -> {}));
                        }))
                ))
        );
    }

    public static NodeTemplate group(String childrenPath) {
        return element("div", root -> root
                .attribute("class", constant("field-group"))
                .child(when(
                        present(path("description")),
                        t -> t.add(
                                element("label", label -> label
                                        .attribute("class", constant("form-label"))
                                        .child(text(path("description")))
                                )
                        )
                ))
                .child(repeat(
                        path(childrenPath),
                        "c",
                        inner -> inner.add(
                                include(
                                        format("field.type.%s", path("c.elementType")),
                                        path("c")
                                )
                        ),
                        ""
                ))
        );
    }

    public static NodeTemplate composite(String childrenPath) {
        return element("div", root -> root
                .attribute("class", constant("mb-3"))
                .child(element("label", label -> label
                        .attribute("class", constant("form-label"))
                        .child(text(path("description")))
                ))
                .child(element("div", row -> row
                        .attribute("class", constant("d-flex flex-wrap gap-2 align-items-end"))
                        .attributes(optional(path("attributes")), "name", "value")
                        .child(repeat(
                                path(childrenPath),
                                "c",
                                inner -> inner.add(
                                        element("div", item -> item
                                                .attribute("class", constant("d-flex flex-column justify-content-end"))
                                                .attribute("style", constant("min-width: 180px;"))
                                                .child(include(
                                                        format("field.type.%s", path("c.elementType")),
                                                        object(
                                                                "name", format("%s[%s]", path("name"), path("c.name")),
                                                                "id", format("%s_%s", path("name"), path("c.name")),
                                                                "description", path("c.description"),
                                                                "label", path("c.label"),
                                                                "options", optional(path("c.options")),
                                                                "attributes", optional(path("c.attributes")),
                                                                "elementType", path("c.elementType")
                                                        )
                                                ))
                                        )
                                ),
                                ""
                        ))
                ))
        );
    }
}