package org.jmouse.dom.meterializer;

import org.jmouse.meterializer.NodeTemplate;
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
        return element("p", block -> block
                .child(element("label", l -> l
                        .attribute("for", path(namePath))
                        .child(text(path(labelPath)))
                ))
                .child(element("br", br -> {}))
                .child(element("input", b -> b
                        .attribute("type", constant(type.htmlValue()))
                        .attribute("id", path(namePath))
                        .attribute("name", path(namePath))
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
        return element("p", block -> block
                .child(element("label", label -> label
                        .attribute("for", path(namePath))
                        .child(text(path(labelPath)))
                ))
                .child(element("br", br -> {}))
                .child(element("select", select -> select
                        .attributes(path("attributes"), "name", "value")
                        .attribute("id", path(namePath))
                        .attribute("name", path(namePath))
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
}