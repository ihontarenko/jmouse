package org.jmouse.kicad.schematic;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.sexpression.node.SExpressionList;

import static org.jmouse.kicad.KiCadKeyword.AT;
import static org.jmouse.kicad.KiCadKeyword.SIZE;

/**
 * A bus entry on a sheet: {@code (bus_entry (at 120.65 69.85) (size 2.54 2.54) …)} — a short slanted stroke from one point to
 * another, one end on a bus line, the other where a wire takes a net off it. It joins nothing: KiCad joins the wire to the bus by
 * the label of a member's name on it.
 *
 * @param at   one of its ends
 * @param size how far the other end is from it, along X and Y
 */
public record KiCadBusEntry(KiCadPoint at, KiCadPoint size) {

    static KiCadBusEntry of(SExpressionList entry) {
        return new KiCadBusEntry(KiCadPoint.of(entry.requireList(AT)), KiCadPoint.of(entry.requireList(SIZE)));
    }

    /** Its other end. */
    public KiCadPoint end() {
        return at.plus(size);
    }
}
