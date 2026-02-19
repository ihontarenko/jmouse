package org.jmouse.template.defaults;

import org.jmouse.template.NodeTemplate;

import static org.jmouse.template.NodeTemplate.*;
import static org.jmouse.template.ValueExpression.*;

public final class StandardBlueprints {

    private StandardBlueprints() {}

    public static NodeTemplate form() {
        return element("form", form -> form
                .attribute("method", request("method"))
                .attribute("action", request("action"))
                .child(repeat(path("fields"), "field", body -> body
                        .add(element("div", container -> container
                                .attribute("data-field", path("field.name"))
                                .child(text("TODO: include field blueprint here"))
                        ))
                ))
        );
    }
}
