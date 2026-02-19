package org.jmouse.dom.template.standard;

import org.jmouse.dom.template.NodeTemplate;

import static org.jmouse.dom.template.build.Blueprints.*;

public final class StandardFieldBlueprints {

    private StandardFieldBlueprints() {}

    public static NodeTemplate textInput() {
        return element("input", input -> input
                .attribute("type", constant("text"))
                .attribute("placeholder", path("label"))
                .attribute("name", path("name"))
                .attribute("id", path("name"))
        );
    }

    public static NodeTemplate textarea() {
        return element("textarea", area -> area
                .attribute("name", path("name"))
                .attribute("id", path("name"))
        );
    }

    public static NodeTemplate select() {
        return element("select", select -> select
                .attribute("name", path("name"))
                .attribute("id", path("name"))
                .child(repeat(path("options"), "option", body -> body.add(
                        element("option", option -> option
                                .attribute("value", path("option.key"))
                                .child(text(path("option.value")))
                        )
                )))
        );
    }

    public static NodeTemplate radioGroup() {
        return element("div", root -> root
                .child(repeat(path("options"), "option", body -> body.add(
                        element("div", wrapper -> wrapper
                                .child(element("input", input -> input
                                        .attribute("type", constant("radio"))
                                        .attribute("name", path("name"))
                                        .attribute("value", path("option.key"))
                                ))
                                .child(element("label", label -> label.child(text(path("option.value")))))
                        )
                )))
        );
    }

    public static NodeTemplate checkboxGroup() {
        return element("div", root -> root
                .child(repeat(path("options"), "option", body -> body.add(
                        element("div", wrapper -> wrapper
                                .child(element("input", input -> input
                                        .attribute("type", constant("checkbox"))
                                        .attribute("name", path("name"))
                                        .attribute("value", path("option.key"))
                                ))
                                .child(element("label", label -> label.child(text(path("option.value")))))
                        )
                )))
        );
    }

    public static NodeTemplate virtual() {
        return text("NO IMPLEMENTATION FOR VIRTUAL COLUMN YET");
    }
}
