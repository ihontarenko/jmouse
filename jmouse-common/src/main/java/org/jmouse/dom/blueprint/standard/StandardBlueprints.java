package org.jmouse.dom.blueprint.standard;

import org.jmouse.dom.blueprint.Blueprint;

import static org.jmouse.dom.blueprint.build.Blueprints.*;

public final class StandardBlueprints {

    private StandardBlueprints() {}

    /**
     * Expects data to contain:
     * - "fields": collection of field objects
     */
    public static Blueprint form() {
        return element("form", form -> form
                .attribute("method", requestAttribute("method"))
                .attribute("action", requestAttribute("action"))
                .child(repeat(path("fields"), "field", body -> body
                        .add(element("div", container -> container
                                .attribute("data-field", path("field.name"))
                                .child(text("TODO: include field blueprint here"))
                        ))
                ))
        );
    }
}
