package org.jmouse.kicad.pcb;

import org.jmouse.kicad.KiCadPoint;

/** The side of a board a footprint stands on, as its copper layer says. */
public enum KiCadSide {

    FRONT("F."),
    BACK("B.");

    private static final String COPPER = "Cu";

    private final String prefix;

    KiCadSide(String prefix) {
        this.prefix = prefix;
    }

    static KiCadSide ofLayer(String layer) {
        return layer.startsWith(BACK.prefix) ? BACK : FRONT;
    }

    /** Its copper layer, {@code F.Cu}. */
    public String copper() {
        return prefix + COPPER;
    }

    /** Its layer of a kind, by the kind's ending: {@code .Mask} on the back is {@code B.Mask}. */
    public String layer(String ending) {
        return prefix.substring(0, prefix.length() - 1) + ending;
    }

    /** A point of a footprint written as it stands on this side, as a library holds it: the back mirrors Y. */
    KiCadPoint unmirrored(KiCadPoint point) {
        return this == BACK ? new KiCadPoint(point.x(), -point.y()) : point;
    }

    /** A layer of a footprint written as it stands on this side, as a library holds it: front's for back's. */
    String libraryLayer(String layer) {
        if (this == BACK && layer.startsWith(BACK.prefix)) {
            return FRONT.prefix + layer.substring(BACK.prefix.length());
        }
        if (this == BACK && layer.startsWith(FRONT.prefix)) {
            return BACK.prefix + layer.substring(FRONT.prefix.length());
        }
        return layer;
    }
}
