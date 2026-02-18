package org.jmouse.dom.blueprint;

/**
 * A predicate used by blueprint nodes.
 */
public sealed interface BlueprintPredicate permits BlueprintPredicate.PathBooleanPredicate {

    /**
     * Boolean predicate resolved from a data path.
     *
     * @param path path expression expected to resolve to boolean
     */
    record PathBooleanPredicate(String path) implements BlueprintPredicate {
    }

}

