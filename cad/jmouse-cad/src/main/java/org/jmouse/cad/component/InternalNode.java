package org.jmouse.cad.component;

/**
 * A node of a component's own, behind its pins — the intrinsic base of a transistor —, and what it is.
 *
 * @param name        what it is called, as the equations name it
 * @param description what it is, for a reader
 */
public record InternalNode(String name, String description) {

    public InternalNode {
        description = description == null ? "" : description;
    }
}
