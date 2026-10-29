package org.jmouse.cad.component;

/** A node of a component's own, behind its pins — the intrinsic base of a transistor —, and what it is. */
public record InternalNode(String name, String description) {

    public InternalNode {
        description = description == null ? "" : description;
    }
}
