package org.jmouse.meterializer.transform;

import org.jmouse.meterializer.TemplateTransformer;

import static org.jmouse.meterializer.transform.Change.*;
import static org.jmouse.meterializer.transform.Match.*;

public final class BootstrapTransformers {

    private BootstrapTransformers() {}

    public static TemplateTransformer create() {
        return RuleBasedTransformer.builder()
                .rule(100, tagName("form"), addClass("dynamic-form"))
                .rule(100, tagName("label"), addClass("form-label"))
                .rule(100, tagName("input"), addClass("form-control"))
                .rule(100, tagName("textarea"), addClass("form-control"))
                .rule(100, tagName("select"), addClass("form-select"))
                .rule(
                        50,
                        and(
                                tagName("select"),
                                not(insideAncestor(a -> constantAttributeValues(a, "data-jm-wrap", "select")))
                        ),
                        chain(
                                // addClass("mb-3"),
                                setAttribute("data-jm-wrap", "select")
                        )
                )
//                .rule(
//                        60,
//                        and(
//                                and(tagName("input"), attributeValue("type", "radio")),
//                                not(insideAncestor(a -> constantAttributeValues(a, "data-jm-wrap", "radio")))
//                        ),
//                        wrapWith("div", chain(
////                                addClass("form-check"),
//                                setAttribute("data-jm-wrap", "radio")
//                        ))
//                )
                .rule(
                        70,
                        and(tagName("input"), attributeValue("type", "radio")),
                        chain(
                                setAttribute("class", "form-check-input")
                        )
                )
                .rule(
                        70,
                        and(tagName("input"), attributeValue("type", "checkbox")),
                        setAttribute("class", "form-check-input")
                )
                .build();
    }
}