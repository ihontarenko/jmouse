package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.*;

import java.util.*;

/**
 * Factory for common blueprint changes.
 */
public final class Change {

    private Change() {}

    public static BlueprintChange renameTag(String newTagName) {
        Verify.nonNull(newTagName, "newTagName");
        return (blueprint, execution) -> {
            if (blueprint instanceof Blueprint.ElementBlueprint element) {
                return new Blueprint.ElementBlueprint(newTagName, element.attributes(), element.children());
            }
            return blueprint;
        };
    }

    public static BlueprintChange setAttribute(String name, BlueprintValue value) {
        Verify.nonNull(name, "name");
        Verify.nonNull(value, "value");
        return (blueprint, execution) -> {
            if (blueprint instanceof Blueprint.ElementBlueprint element) {
                Map<String, BlueprintValue> attributes = new LinkedHashMap<>(element.attributes());
                attributes.put(name, value);
                return new Blueprint.ElementBlueprint(element.tagName(), Map.copyOf(attributes), element.children());
            }
            return blueprint;
        };
    }

    public static BlueprintChange setAttribute(String name, Object constantValue) {
        return setAttribute(name, new BlueprintValue.ConstantBlueprintValue(constantValue));
    }

    public static BlueprintChange addClass(String className) {
        Verify.nonNull(className, "className");
        return (blueprint, execution) -> {
            if (blueprint instanceof Blueprint.ElementBlueprint(
                    String tagName, Map<String, BlueprintValue> elementAttributes, List<Blueprint> children
            )) {
                Map<String, BlueprintValue> attributes = new LinkedHashMap<>(elementAttributes);
                BlueprintValue              current    = attributes.get("class");
                String                      merged     = mergeClass(current, className);

                attributes.put("class", new BlueprintValue.ConstantBlueprintValue(merged));

                return new Blueprint.ElementBlueprint(tagName, Map.copyOf(attributes), children);
            }
            return blueprint;
        };
    }

    public static BlueprintChange wrapWith(String wrapperTagName, BlueprintChange wrapperChange) {
        Verify.nonNull(wrapperTagName, "wrapperTagName");
        Verify.nonNull(wrapperChange, "wrapperChange");
        return (blueprint, execution) -> {
            Blueprint.ElementBlueprint wrapper        = new Blueprint.ElementBlueprint(wrapperTagName, Map.of(), List.of(blueprint));
            Blueprint                  changed = wrapperChange.apply(wrapper, execution);
            return changed;
        };
    }

    public static BlueprintChange appendChild(Blueprint child) {
        Verify.nonNull(child, "child");
        return (blueprint, execution) -> {
            if (blueprint instanceof Blueprint.ElementBlueprint(
                    String tagName, Map<String, BlueprintValue> attributes, List<Blueprint> elementChildren
            )) {
                List<Blueprint> children = new ArrayList<>(elementChildren);
                children.add(child);
                return new Blueprint.ElementBlueprint(tagName, attributes, List.copyOf(children));
            }
            return blueprint;
        };
    }

    public static BlueprintChange prependChild(Blueprint child) {
        Verify.nonNull(child, "child");
        return (blueprint, execution) -> {
            if (blueprint instanceof Blueprint.ElementBlueprint(
                    String tagName, Map<String, BlueprintValue> attributes, List<Blueprint> elementChildren
            )) {
                List<Blueprint> children = new ArrayList<>();
                children.add(child);
                children.addAll(elementChildren);
                return new Blueprint.ElementBlueprint(tagName, attributes, List.copyOf(children));
            }
            return blueprint;
        };
    }

    public static BlueprintChange chain(BlueprintChange... changes) {
        Verify.nonNull(changes, "changes");
        return (blueprint, execution) -> {
            Blueprint current = blueprint;
            for (BlueprintChange change : changes) {
                current = change.apply(current, execution);
            }
            return current;
        };
    }

    private static String mergeClass(BlueprintValue current, String add) {
        if (current instanceof BlueprintValue.ConstantBlueprintValue(Object constant)) {
            String existing = String.valueOf(constant).trim();
            if (existing.isEmpty()) {
                return add;
            }
            if (containsToken(existing, add)) {
                return existing;
            }
            return "%s %s".formatted(existing, add);
        }
        return add;
    }

    private static boolean containsToken(String classes, String token) {
        for (String chunk : classes.split("\\s+")) {
            if (chunk.equals(token)) {
                return true;
            }
        }
        return false;
    }
}
