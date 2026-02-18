package org.jmouse.dom.blueprint;

import java.util.List;
import java.util.Map;

/**
 * Intermediate representation of a markup tree that can be transformed before materialization.
 */
public sealed interface Blueprint
        permits Blueprint.ElementBlueprint,
                Blueprint.TextBlueprint,
                Blueprint.ConditionalBlueprint,
                Blueprint.RepeatBlueprint,
                Blueprint.IncludeBlueprint {

    /**
     * Represents an element node in a blueprint tree.
     *
     * @param tagName element tag name (for example "div", "input", "select")
     * @param attributes element attributes; values can be constant or bound to a data path
     * @param children child blueprint nodes
     */
    record ElementBlueprint(
            String tagName,
            Map<String, BlueprintValue> attributes,
            List<Blueprint> children,
            List<BlueprintDirective> directives
    ) implements Blueprint {
    }

    /**
     * Represents a text node in a blueprint tree.
     *
     * @param value constant or bound value
     */
    record TextBlueprint(BlueprintValue value) implements Blueprint {
    }

    /**
     * Represents a conditional branch in a blueprint tree.
     *
     * @param predicate predicate that decides which branch to materialize
     * @param whenTrue nodes materialized when predicate is true
     * @param whenFalse nodes materialized when predicate is false
     */
    record ConditionalBlueprint(
            BlueprintPredicate predicate,
            List<Blueprint> whenTrue,
            List<Blueprint> whenFalse
    ) implements Blueprint {
    }

    /**
     * Represents a repeatable section in a blueprint tree.
     *
     * @param collection value that resolves to a collection-like object
     * @param itemVariableName variable name used inside the body
     * @param body blueprint nodes to be materialized for each element
     */
    record RepeatBlueprint(
            BlueprintValue collection,
            String itemVariableName,
            List<Blueprint> body
    ) implements Blueprint {
    }

    /**
     * Includes another blueprint by key.
     *
     * @param blueprintKey key expression (can be constant or path-bound)
     * @param model model expression (object to pass as new root; can be path-bound)
     */
    record IncludeBlueprint(
            BlueprintValue blueprintKey,
            BlueprintValue model
    ) implements Blueprint {}

}
