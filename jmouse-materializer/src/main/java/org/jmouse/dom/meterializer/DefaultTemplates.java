package org.jmouse.dom.meterializer;

import org.jmouse.dom.InputType;
import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.util.Strings;

import static org.jmouse.meterializer.NodeTemplate.*;
import static org.jmouse.meterializer.TemplatePredicate.present;
import static org.jmouse.meterializer.ValueExpression.*;

public final class DefaultTemplates {

    private DefaultTemplates() {}

    public static NodeTemplate defaultForm() {
        return defaultForm("", "POST");
    }

    public static NodeTemplate defaultForm(String action, String method) {
        return element("form", form -> form
                .child(element("style", s -> s
                        .child(text(".composite-items .composite-item {min-width: 160px;} .composite-items .composite-item:last-child {flex: 1 1 220px;}"))
                ))
                .attribute("method", constant(method.toUpperCase()))
                .attribute("action", constant(action))
                .attribute("class", constant("p-3"))

                .child(element("h3", h -> h.child(text(path("description")))))

                .child(element("p", p -> p
                        .attribute("class", constant("text-muted"))
                        .child(text(path("description")))
                ))

                .child(repeat(
                        path("fields"),
                        "field",
                        body -> body.add(
                                include(
                                        format("field.type.%s", path("field.elementType")),
                                        path("field")
                                )
                        ).add(element("hr", __ -> {}))
                ))

                .child(element("hr", __ -> {}))

                .child(include(
                        constant("default.button.submit"),
                        root()
                ))

                .child(element("hr", __ -> {}))
        );
    }

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
        return element("div", block -> block
                .attribute("class", constant("form-check mb-3"))
                .child(element("input", input -> {
                    input.attribute("type", constant("checkbox"));
                    input.attribute("class", constant("form-check-input"));
                    input.attribute("id", path("id"));
                    input.attribute("name", path(namePath));

                    if (Strings.isNotEmpty(valuePath)) {
                        input.attribute("value", path(valuePath));
                    }
                }))
                .child(element("label", label -> label
                        .attribute("class", constant("form-check-label"))
                        .attribute("for", path("id"))
                        .child(text(path(labelPath)))
                ))
        );
    }

    public static NodeTemplate checkboxInline(String namePath, String labelPath, String valuePath) {
        return element("div", block -> block
                .attribute("class", constant("form-check form-check-inline"))
                .child(element("input", input -> {
                    input.attribute("type", constant("checkbox"));
                    input.attribute("class", constant("form-check-input"));
                    input.attribute("id", path("id"));
                    input.attribute("name", path(namePath));

                    if (Strings.isNotEmpty(valuePath)) {
                        input.attribute("value", path(valuePath));
                    }
                }))
                .child(element("label", label -> label
                        .attribute("class", constant("form-check-label"))
                        .attribute("for", path("id"))
                        .child(text(path(labelPath)))
                ))
        );
    }

    public static NodeTemplate checkboxGroup(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", block -> block
                .attribute("class", constant("mb-3"))
                .child(element("label", label -> label
                        .attribute("class", constant("form-label d-block"))
                        .child(text(path(labelPath)))
                ))
                .child(repeat(
                        path(optionsPath),
                        "option",
                        inner -> inner.add(
                                element("div", item -> item
                                        .attribute("class", constant("form-check"))
                                        .child(element("input", input -> input
                                                .attribute("type", constant("checkbox"))
                                                .attribute("class", constant("form-check-input"))
                                                .attribute("id", format("%s_%s", path("id"), path(keyPath)))
                                                .attribute("name", path(namePath))
                                                .attribute("value", path(keyPath))
                                        ))
                                        .child(element("label", label -> label
                                                .attribute("class", constant("form-check-label"))
                                                .attribute("for", format("%s_%s", path("id"), path(keyPath)))
                                                .child(text(path(valuePath)))
                                        ))
                                )
                        ),
                        ""
                ))
        );
    }

    public static NodeTemplate radioGroup(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", fieldset -> fieldset
                .attribute("class", constant("mb-3"))

                .child(element("label", label -> label
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))

                .child(repeat(
                        path(optionsPath),
                        "option",
                        inner -> inner.add(
                                element("div", wrapper -> wrapper
                                        .attribute("class", constant("form-check"))

                                        .child(element("input", input -> input
                                                .attribute("type", constant("radio"))
                                                .attribute("class", constant("form-check-input"))
                                                .attribute("name", path(namePath))
                                                .attribute("value", path(keyPath))
                                                .attribute("id", format("%s_%s", path("id"), path(keyPath)))
                                        ))

                                        .child(element("label", label -> label
                                                .attribute("class", constant("form-check-label"))
                                                .attribute("for", format("%s_%s", path("id"), path(keyPath)))
                                                .child(text(path(valuePath)))
                                        ))
                                )
                        ),
                        ""
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