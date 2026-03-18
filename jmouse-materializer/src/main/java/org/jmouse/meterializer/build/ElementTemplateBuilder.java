package org.jmouse.meterializer.build;

import org.jmouse.meterializer.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;

/**
 * Builder for {@link NodeTemplate.Element}.
 */
public final class ElementTemplateBuilder {

    private final QName                        qName;
    private final Map<String, ValueExpression> attributes = new LinkedHashMap<>();
    private final List<NodeTemplate>           children   = new ArrayList<>();
    private final List<NodeDirective>          directives = new ArrayList<>();

    public ElementTemplateBuilder(QName qName) {
        this.qName = nonNull(qName, "qName");
    }

    public ElementTemplateBuilder attributes(
            ValueExpression source, String keyValue, String valueKey
    ) {
        return directive(new NodeDirective.ApplyAttributes(
                nonNull(source, "source"),
                nonNull(keyValue, "keyValue"),
                nonNull(valueKey, "valueKey")
        ));
    }

    public ElementTemplateBuilder attribute(String name, ValueExpression value) {
        attributes.put(nonNull(name, "name"), nonNull(value, "value"));
        return this;
    }

    public ElementTemplateBuilder attribute(String name, Object constantValue) {
        return attribute(name, ValueExpression.constant(constantValue));
    }

    public ElementTemplateBuilder child(NodeTemplate child) {
        nonNull(child, "child");
        children.add(child);
        return this;
    }

    public ElementTemplateBuilder children(List<NodeTemplate> children) {
        nonNull(children, "children");
        this.children.addAll(children);
        return this;
    }

    public ElementTemplateBuilder directive(NodeDirective directive) {
        nonNull(directive, "directive");
        directives.add(directive);
        return this;
    }

    public ElementTemplateBuilder attributeIf(TemplatePredicate predicate, String name, ValueExpression value) {
        return directive(new NodeDirective.SetAttributeIf(predicate, name, value));
    }

    public ElementTemplateBuilder removeAttributeIf(TemplatePredicate predicate, String attributeName) {
        return directive(new NodeDirective.RemoveAttributeIf(predicate, attributeName));
    }

    public ElementTemplateBuilder classIf(TemplatePredicate predicate, ValueExpression classValue) {
        return directive(new NodeDirective.AddClassIf(predicate, classValue));
    }

    public ElementTemplateBuilder wrapIf(
            TemplatePredicate predicate, String wrapperTagName, Map<String, ValueExpression> wrapperAttributes) {
        return directive(new NodeDirective.WrapIf(predicate, wrapperTagName, wrapperAttributes));
    }

    public ElementTemplateBuilder omitIf(TemplatePredicate predicate) {
        return directive(new NodeDirective.OmitIf(predicate));
    }

    public NodeTemplate.Element build() {
        return new NodeTemplate.Element(
                qName, Map.copyOf(attributes), List.copyOf(children), List.copyOf(directives)
        );
    }
}
