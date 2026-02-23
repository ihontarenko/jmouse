package org.jmouse.meterializer.transform;

import org.jmouse.meterializer.TemplateTransformer;

import static org.jmouse.meterializer.transform.Change.*;
import static org.jmouse.meterializer.transform.Match.*;

/**
 * Bootstrap-oriented transformer set.
 */
public final class BootstrapTransformers {

    private BootstrapTransformers() {}

    public static TemplateTransformer create() {
        return RuleBasedTransformer.builder()

                .rule(100, tagName("input"), addClass("form-control"))
                .rule(100, tagName("select"), addClass("form-select"))
                .rule(100, tagName("textarea"), addClass("form-control"))
                .rule(100, tagName("label"), addClass("form-label bootstrap-label"))
                .rule(100, tagName("form"), addClass("dynamic-form"))

                .rule(50, tagName("select"),
                      wrapWith("div", addClass("mb-3 extended-select xx-1")))

                .rule(
                        100,
                        and(and(
                                tagName("input"),
                                attributeValue("type", "radio")
                        ), not(insideAncestor(a -> a.tagName().equalsIgnoreCase("div")
                                && constantAttributeValues(a, "data-jm-wrap", "radio")))),
                        wrapWith("div", chain(
                                addClass("mb-3 extended-radio"),
                                setAttribute("data-jm-wrap", "radio")
                        ))
                )

                .build();
    }
}
