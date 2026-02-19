package org.jmouse.dom.blueprint.standard;

import org.jmouse.dom.blueprint.Blueprint;
import org.jmouse.util.Strings;

import static org.jmouse.dom.blueprint.build.Blueprints.*;

/**
 * Bootstrap-styled presets.
 *
 * <p>Uses HTML5Preset primitives but adds Bootstrap class names and wrappers.</p>
 */
public final class BootstrapPreset {

    private BootstrapPreset() {}

    public static Blueprint input(InputType type, String namePath, String labelPath, String valuePath) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("label", label -> label
                        .attribute("for", path(namePath))
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(element("input", input -> {
                    input.attribute("type", constant(type.htmlValue()));
                    input.attribute("name", path(namePath));
                    input.attribute("id", path(namePath));
                    input.attribute("class", constant("form-control preset-class"));

                    if (Strings.isNotEmpty(valuePath)) {
                        input.attribute("value", path(valuePath));
                    }
                }))
        );
    }

    public static Blueprint inputText(String namePath, String labelPath, String valuePath) {
        return input(InputType.TEXT, namePath, labelPath, valuePath);
    }

    public static Blueprint inputNumber(String namePath, String labelPath, String valuePath) {
        return input(InputType.NUMBER, namePath, labelPath, valuePath);
    }

    public static Blueprint inputEmail(String namePath, String labelPath, String valuePath) {
        return input(InputType.EMAIL, namePath, labelPath, valuePath);
    }

    public static Blueprint inputPassword(String namePath, String labelPath, String valuePath) {
        return input(InputType.PASSWORD, namePath, labelPath, valuePath);
    }

    public static Blueprint select(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("label", label -> label
                        .attribute("for", path(namePath))
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(element("select", select -> select
                        .attribute("name", path(namePath))
                        .attribute("id", path(namePath))
                        .attribute("class", constant("form-select"))
                        .child(repeat(
                                path(optionsPath),
                                "option",
                                inner -> inner.add(
                                        element("option", option -> option
                                                .attribute("value", path(keyPath))
                                                .child(text(path(valuePath)))
                                        )
                                )
                        ))
                ))
        );
    }

    public static Blueprint checkboxSingle(String namePath, String labelPath, String valuePath) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("div", row -> row
                        .attribute("class", constant("form-check"))
                        .child(element("input", input -> {
                            input.attribute("type", constant("checkbox"));
                            input.attribute("class", constant("form-check-input"));
                            input.attribute("name", path(namePath));
                            input.attribute("id", path(namePath));
                            if (Strings.isNotEmpty(valuePath)) {
                                input.attribute("value", path(valuePath));
                            }
                        }))
                        .child(element("label", label -> label
                                .attribute("class", constant("form-check-label"))
                                .attribute("for", path(namePath))
                                .child(text(path(labelPath)))
                        ))
                ))
        );
    }

    public static Blueprint radioGroup(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("div", title -> title
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(repeat(
                        path(optionsPath),
                        "option",
                        inner -> inner.add(element("div", row -> row
                                .attribute("class", constant("form-check"))
                                .child(element("input", input -> input
                                        .attribute("type", constant("radio"))
                                        .attribute("class", constant("form-check-input"))
                                        .attribute("name", path(namePath))
                                        .attribute("value", path(keyPath))
                                ))
                                .child(element("label", label -> label
                                        .attribute("class", constant("form-check-label"))
                                        .child(text(path(valuePath)))
                                ))
                        ))
                ))
        );
    }

    public static Blueprint checkbox(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("div", title -> title
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(repeat(
                        path(optionsPath),
                        "option",
                        inner -> inner.add(element("div", row -> row
                                .attribute("class", constant("form-check"))
                                .child(when(
                                        contains(path(optionsPath), path("option.key")),
                                        whenTrue -> whenTrue.add(checkboxInput(namePath, keyPath, true)),
                                        whenFalse -> whenFalse.add(checkboxInput(namePath, keyPath, false))
                                ))
                                .child(element("label", label -> label
                                        .attribute("class", constant("form-check-label"))
                                        .child(text(path(valuePath)))
                                ))
                        ))
                ))
        );
    }

    private static Blueprint checkboxInput(String namePath, String keyPath, boolean checked) {
        return element("input", input -> {
            input.attribute("type", constant("checkbox"));
            input.attribute("class", constant("form-check-input"));
            input.attribute("name", path(namePath));
            input.attributeIf(same(constant("a"), constant("a")), "type", constant("checkbox"));
            input.attribute("value", path(keyPath));
            if (checked) {
                input.attribute("checked", constant("checked"));
            }
        });
    }

}
