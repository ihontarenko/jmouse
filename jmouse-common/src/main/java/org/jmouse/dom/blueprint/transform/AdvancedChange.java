package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.*;
import org.jmouse.dom.blueprint.Blueprint.ElementBlueprint;

import java.util.*;

/**
 * Advanced blueprint changes.
 */
public final class AdvancedChange {

    private AdvancedChange() {}

    public static BlueprintChange removeAttribute(String name) {
        Verify.nonNull(name, "name");
        return (blueprint, execution) -> {
            if (blueprint instanceof ElementBlueprint element) {
                Map<String, BlueprintValue> attributes = new LinkedHashMap<>(element.attributes());
                attributes.remove(name);
                return new ElementBlueprint(element.tagName(), Map.copyOf(attributes), element.children());
            }
            return blueprint;
        };
    }

    public static BlueprintChange mergeAttributes(Map<String, BlueprintValue> addOrReplace) {
        Verify.nonNull(addOrReplace, "addOrReplace");
        return (blueprint, execution) -> {
            if (blueprint instanceof ElementBlueprint element) {
                Map<String, BlueprintValue> attributes = new LinkedHashMap<>(element.attributes());
                attributes.putAll(addOrReplace);
                return new ElementBlueprint(element.tagName(), Map.copyOf(attributes), element.children());
            }
            return blueprint;
        };
    }

    public static BlueprintChange replaceChildren(List<Blueprint> children) {
        Verify.nonNull(children, "children");
        return (blueprint, execution) -> {
            if (blueprint instanceof ElementBlueprint element) {
                return new ElementBlueprint(element.tagName(), element.attributes(), List.copyOf(children));
            }
            return blueprint;
        };
    }

    public static BlueprintChange wrapChildrenWith(String wrapperTagName, BlueprintChange wrapperChange) {
        Verify.nonNull(wrapperTagName, "wrapperTagName");
        Verify.nonNull(wrapperChange, "wrapperChange");
        return (blueprint, execution) -> {
            if (blueprint instanceof ElementBlueprint element) {
                ElementBlueprint wrapper = new ElementBlueprint(wrapperTagName, Map.of(), element.children());
                Blueprint changedWrapper = wrapperChange.apply(wrapper, execution);
                return new ElementBlueprint(element.tagName(), element.attributes(), List.of(changedWrapper));
            }
            return blueprint;
        };
    }

    public static BlueprintChange unwrapSingleChild() {
        return (blueprint, execution) -> {
            if (blueprint instanceof ElementBlueprint element) {
                if (element.children().size() == 1) {
                    return element.children().get(0);
                }
            }
            return blueprint;
        };
    }
}
