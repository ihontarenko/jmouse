package org.jmouse.dom.blueprint.build;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.Blueprint;
import org.jmouse.dom.blueprint.BlueprintDirective;
import org.jmouse.dom.blueprint.BlueprintPredicate;
import org.jmouse.dom.blueprint.BlueprintValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for {@link org.jmouse.dom.blueprint.Blueprint.ElementBlueprint}.
 */
public final class ElementBlueprintBuilder {

    private final String                      tagName;
    private final Map<String, BlueprintValue> attributes = new LinkedHashMap<>();
    private final List<Blueprint>             children   = new ArrayList<>();
    private final List<BlueprintDirective>    directives = new ArrayList<>();

    public ElementBlueprintBuilder(String tagName) {
        this.tagName = Verify.nonNull(tagName, "tagName");
    }

    public ElementBlueprintBuilder attribute(String name, BlueprintValue value) {
        Verify.nonNull(name, "name");
        Verify.nonNull(value, "value");
        attributes.put(name, value);
        return this;
    }

    public ElementBlueprintBuilder attribute(String name, Object constantValue) {
        return attribute(name, Blueprints.constant(constantValue));
    }

    public ElementBlueprintBuilder child(Blueprint child) {
        Verify.nonNull(child, "child");
        children.add(child);
        return this;
    }

    public ElementBlueprintBuilder children(List<Blueprint> children) {
        Verify.nonNull(children, "children");
        this.children.addAll(children);
        return this;
    }

    public ElementBlueprintBuilder directive(BlueprintDirective directive) {
        Verify.nonNull(directive, "directive");
        directives.add(directive);
        return this;
    }

    public ElementBlueprintBuilder attributeIf(BlueprintPredicate predicate, String name, BlueprintValue value) {
        return directive(new BlueprintDirective.SetAttributeIf(predicate, name, value));
    }

    public ElementBlueprintBuilder removeAttributeIf(BlueprintPredicate predicate, String attributeName) {
        return directive(new BlueprintDirective.RemoveAttributeIf(predicate, attributeName));
    }

    public ElementBlueprintBuilder classIf(BlueprintPredicate predicate, BlueprintValue classValue) {
        return directive(new BlueprintDirective.AddClassIf(predicate, classValue));
    }

    public ElementBlueprintBuilder wrapIf(
            BlueprintPredicate predicate, String wrapperTagName, Map<String, BlueprintValue> wrapperAttributes) {
        return directive(new BlueprintDirective.WrapIf(predicate, wrapperTagName, wrapperAttributes));
    }

    public ElementBlueprintBuilder omitIf(BlueprintPredicate predicate) {
        return directive(new BlueprintDirective.OmitIf(predicate));
    }

    public Blueprint.ElementBlueprint build() {
        return new Blueprint.ElementBlueprint(
                tagName, Map.copyOf(attributes), List.copyOf(children), List.copyOf(directives)
        );
    }
}
