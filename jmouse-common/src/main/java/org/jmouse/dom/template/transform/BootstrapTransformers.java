package org.jmouse.dom.template.transform;

import org.jmouse.dom.template.TemplateTransformer;

import static org.jmouse.dom.template.transform.Change.*;
import static org.jmouse.dom.template.transform.Match.*;

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
                      wrapWith("div", addClass("mb-3 extended-select")))

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
