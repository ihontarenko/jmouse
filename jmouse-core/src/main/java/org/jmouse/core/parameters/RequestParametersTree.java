package org.jmouse.core.parameters;

/**
 * 🌳 Names and their values as one tree: a record at its root — or a list, where the names begin with an index.
 */
public record RequestParametersTree(RequestParametersNode root) {

    public RequestParametersTree {
        if (!(root instanceof RequestParametersObjectNode) && !(root instanceof RequestParametersArrayNode)) {
            throw new IllegalArgumentException("A tree's root is a record or a list: a value is a parameter only under a name.");
        }
    }
}
