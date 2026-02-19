package org.jmouse.meterializer.transform;

import org.jmouse.core.Verify;
import org.jmouse.meterializer.NodeDirective;
import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.meterializer.NodeTemplate.Element;
import org.jmouse.meterializer.ValueExpression;

import java.util.*;

/**
 * Advanced blueprint changes.
 */
public final class AdvancedChange {

    private AdvancedChange() {}

    public static TemplateChange removeAttribute(String name) {
        Verify.nonNull(name, "name");
        return (blueprint, execution) -> {
            if (blueprint instanceof Element(
                    String tagName, Map<String,
                    ValueExpression> elementAttributes,
                    List<NodeTemplate> children,
                    List<NodeDirective> directives
            )) {
                Map<String, ValueExpression> attributes = new LinkedHashMap<>(elementAttributes);
                attributes.remove(name);
                return new Element(tagName, Map.copyOf(attributes), children, directives);
            }
            return blueprint;
        };
    }

    public static TemplateChange mergeAttributes(Map<String, ValueExpression> addOrReplace) {
        Verify.nonNull(addOrReplace, "addOrReplace");
        return (blueprint, execution) -> {
            if (blueprint instanceof Element(
                    String tagName,
                    Map<String, ValueExpression> elementAttributes,
                    List<NodeTemplate> children,
                    List<NodeDirective> directives
            )) {
                Map<String, ValueExpression> attributes = new LinkedHashMap<>(elementAttributes);
                attributes.putAll(addOrReplace);
                return new Element(
                        tagName, Map.copyOf(attributes), children, directives);
            }
            return blueprint;
        };
    }

    public static TemplateChange replaceChildren(List<NodeTemplate> children) {
        Verify.nonNull(children, "children");
        return (blueprint, execution) -> {
            if (blueprint instanceof Element element) {
                return new Element(
                        element.tagName(), element.attributes(), List.copyOf(children), element.directives());
            }
            return blueprint;
        };
    }

    public static TemplateChange wrapChildrenWith(String wrapperTagName, TemplateChange wrapperChange) {
        Verify.nonNull(wrapperTagName, "wrapperTagName");
        Verify.nonNull(wrapperChange, "wrapperChange");
        return (blueprint, execution) -> {
            if (blueprint instanceof Element(
                    String tagName,
                    Map<String, ValueExpression> attributes,
                    List<NodeTemplate> children,
                    List<NodeDirective> directives
            )) {
                Element      wrapper = new Element(wrapperTagName, Map.of(), children, directives);
                NodeTemplate changed = wrapperChange.apply(wrapper, execution);
                return new Element(tagName, attributes, List.of(changed), directives);
            }
            return blueprint;
        };
    }

    public static TemplateChange unwrapSingleChild() {
        return (blueprint, execution) -> {
            if (blueprint instanceof Element element) {
                if (element.children().size() == 1) {
                    return element.children().getFirst();
                }
            }
            return blueprint;
        };
    }
}
