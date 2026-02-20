package org.jmouse.meterializer;

import org.jmouse.meterializer.build.Include;

import static org.jmouse.meterializer.NodeTemplate.element;
import static org.jmouse.meterializer.NodeTemplate.text;
import static org.jmouse.meterializer.ValueExpression.constant;
import static org.jmouse.meterializer.ValueExpression.path;

public final class Templates {

    private Templates() {}

    public static NodeTemplate bootstrapForm() {
        return element("form", form -> form
                .attribute("method", constant("POST"))
                .attribute("class", constant("p-3"))

                .child(element(QName.of("https://prom.ua/ua/", "ns1", "h3"), h -> h.child(text(path("title")))))

                .child(element("p", p -> p
                        .attribute("class", constant("text-muted"))
                        .child(text(path("description")))
                ))

                // Universal: repeat blocks, include per-field template
                .child(NodeTemplate.repeat(
                        path("fields"),
                        "field",
                        body -> body.add(
                                Include.template(
                                        path("field.templateKey"),
                                        path("field.model")
                                )
                        )
                ))

                // Submit
                .child(Include.template(
                        constant("uni/button/submit"),
                        path("submit")
                ))

                .child(element("script", s -> s
                        .attribute("src", constant("https://code.jquery.com/jquery-4.0.0.js"))
                        .attribute("integrity", constant("sha256-9fsHeVnKBvqh3FB2HYu7g2xseAZ5MlN6Kz/qnkASV8U="))
                        .attribute("crossorigin", constant("anonymous"))
                ))
        );
    }

    public static NodeTemplate html5Form() {
        return element("form", form -> form
                .attribute("method", constant("POST"))

                .child(element("h3", h -> h.child(text(path("title")))))

                .child(element("p", p -> p.child(text(path("description")))))

                // Universal: same contract as bootstrap
                .child(NodeTemplate.repeat(
                        path("fields"),
                        "field",
                        body -> body.add(
                                Include.template(
                                        path("field.templateKey"),
                                        path("field.model")
                                )
                        )
                ))

                // Submit (same key so you can swap only the submit template per theme in registry)
                .child(Include.template(
                        constant("uni/button/submit"),
                        path("submit")
                ))
        );
    }
}
