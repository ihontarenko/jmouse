package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

/** A point where lines of a segment meet, or one ends: on a sheet and on a board alike. */
public record LibrePcbJunction(String id, LibrePcbPoint at) {

    static LibrePcbJunction of(SExpressionList junction) {
        return new LibrePcbJunction(junction.word(1), LibrePcbValues.position(junction));
    }
}
