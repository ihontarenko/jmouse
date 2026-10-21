package org.jmouse.tree;

/**
 * 🌳 A change the tree refuses because the result would not be a tree: a node moved into its own subtree,
 * a node moved between two trees, a parent pointer leading nowhere or round in a circle.
 *
 * <p>A host usually refuses these first, in its own words and with its own exception; this is the floor
 * beneath that — what keeps the numbering from being written wrong when a host forgot to look.</p>
 */
public class TreeException extends RuntimeException {

    public TreeException(String message) {
        super(message);
    }
}
