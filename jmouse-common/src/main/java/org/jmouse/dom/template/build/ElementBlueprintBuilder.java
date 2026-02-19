package org.jmouse.dom.template.build;

import org.jmouse.core.Verify;
import org.jmouse.dom.template.NodeTemplate;
import org.jmouse.dom.template.NodeDirective;
import org.jmouse.dom.template.TemplatePredicate;
import org.jmouse.dom.template.ValueExpression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for {@link NodeTemplate.Element}.
 */
public final class ElementBlueprintBuilder {

    private final String                       tagName;
    private final Map<String, ValueExpression> attributes = new LinkedHashMap<>();
    private final List<NodeTemplate>           children   = new ArrayList<>();
    private final List<NodeDirective>          directives = new ArrayList<>();

    public ElementBlueprintBuilder(String tagName) {
        this.tagName = Verify.nonNull(tagName, "tagName");
    }

    public ElementBlueprintBuilder attribute(String name, ValueExpression value) {
        Verify.nonNull(name, "name");
        Verify.nonNull(value, "value");
        attributes.put(name, value);
        return this;
    }

    public ElementBlueprintBuilder attribute(String name, Object constantValue) {
        return attribute(name, Blueprints.constant(constantValue));
    }

    public ElementBlueprintBuilder child(NodeTemplate child) {
        Verify.nonNull(child, "child");
        children.add(child);
        return this;
    }

    public ElementBlueprintBuilder children(List<NodeTemplate> children) {
        Verify.nonNull(children, "children");
        this.children.addAll(children);
        return this;
    }

    public ElementBlueprintBuilder directive(NodeDirective directive) {
        Verify.nonNull(directive, "directive");
        directives.add(directive);
        return this;
    }

    public ElementBlueprintBuilder attributeIf(TemplatePredicate predicate, String name, ValueExpression value) {
        return directive(new NodeDirective.SetAttributeIf(predicate, name, value));
    }

    public ElementBlueprintBuilder removeAttributeIf(TemplatePredicate predicate, String attributeName) {
        return directive(new NodeDirective.RemoveAttributeIf(predicate, attributeName));
    }

    public ElementBlueprintBuilder classIf(TemplatePredicate predicate, ValueExpression classValue) {
        return directive(new NodeDirective.AddClassIf(predicate, classValue));
    }

    public ElementBlueprintBuilder wrapIf(
            TemplatePredicate predicate, String wrapperTagName, Map<String, ValueExpression> wrapperAttributes) {
        return directive(new NodeDirective.WrapIf(predicate, wrapperTagName, wrapperAttributes));
    }

    public ElementBlueprintBuilder omitIf(TemplatePredicate predicate) {
        return directive(new NodeDirective.OmitIf(predicate));
    }

    public NodeTemplate.Element build() {
        return new NodeTemplate.Element(
                tagName, Map.copyOf(attributes), List.copyOf(children), List.copyOf(directives)
        );
    }
}
