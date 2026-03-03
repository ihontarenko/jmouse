package org.jmouse.meterializer;

import static org.jmouse.meterializer.NodeTemplate.*;
import static org.jmouse.meterializer.ValueExpression.*;

public final class Templates {

    private Templates() {}

    public static NodeTemplate defaultForm() {
        return defaultForm("", "POST");
    }

    public static NodeTemplate defaultForm(String action, String method) {
        return element("form", form -> form
                .attribute("method", constant(method.toUpperCase()))
                .attribute("action", constant(action))
                .attribute("class", constant("p-3"))

                .child(element("h3", h -> h.child(text(path("description")))))

                .child(element("p", p -> p
                        .attribute("class", constant("text-muted"))
                        .child(text(path("description")))
                ))

                // Universal: repeat blocks, include per-field template
                .child(repeat(
                        path("fields"),
                        "field",
                        body -> body.add(
                                include(
                                        format("field.type.%s", path("field.elementType")),
                                        path("field")
                                )
                        )
                ))

                .child(element("hr", __ -> {}))

                .child(include(
                        constant("default.button.submit"),
                        root()
                ))

                .child(element("hr", __ -> {}))
        );
    }

    public static NodeTemplate html5Form() {
        return element("form", form -> form
                .attribute("method", constant("POST"))

                .child(element("h3", h -> h.child(text(path("title")))))

                .child(element("p", p -> p.child(text(path("description")))))

                .child(repeat(
                        path("fields"),
                        "field",
                        body -> body.add(
                                include(
                                        path("field.templateReference"),
                                        path("field.model")
                                )
                        )
                ))

                .child(include(
                        constant("default.button.submit"),
                        path("")
                ))
        );
    }
}
