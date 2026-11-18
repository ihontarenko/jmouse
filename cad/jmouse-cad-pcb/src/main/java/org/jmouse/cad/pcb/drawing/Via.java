package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.drawing.Arrangement;
import java.util.List;

/**
 * A via: a plated hole joining copper layers.
 *
 * @param x           where it stands, millimetres from the left
 * @param y           where it stands, millimetres down
 * @param drill       its hole's diameter, millimetres
 * @param diameter    its copper ring's diameter, millimetres
 * @param from        the layer it starts on; empty for through the pcb
 * @param to          the layer it ends on; empty for through the pcb
 * @param open        where the solder mask is left open over it: {@code top}, {@code bottom} or {@code both}; empty for covered
 * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
 */
public record Via(double x, double y, double drill, double diameter, String from, String to, String open, Arrangement arrangement) {

    public Via {
        arrangement = arrangement == null ? Arrangement.FREE : arrangement;
    }

    /** A via in no group, free. */
    public Via(double x, double y, double drill, double diameter, String from, String to, String open) {
        this(x, y, drill, diameter, from, to, open, Arrangement.FREE);
    }

    /** The same via standing otherwise among the others. */
    public Via arranged(Arrangement other) {
        return new Via(x, y, drill, diameter, from, to, open, other);
    }

    /** The same via standing at another point. */
    public Via at(double otherX, double otherY) {
        return new Via(otherX, otherY, drill, diameter, from, to, open, arrangement);
    }

    /** The copper layers it passes through, of a pcb's from the top down: from its first to its last. */
    public List<String> layersOf(List<String> copperLayers) {
        // A via named by no layers goes through the pcb; an unmodifiable list refuses to look for null.
        if (from == null || to == null) {
            return copperLayers;
        }
        int first = copperLayers.indexOf(from);
        int last = copperLayers.indexOf(to);
        if (first < 0 || last < 0) {
            return copperLayers;
        }
        return copperLayers.subList(Math.min(first, last), Math.max(first, last) + 1);
    }

    /** A via covered by the solder mask on both sides. */
    public Via(double x, double y, double drill, double diameter, String from, String to) {
        this(x, y, drill, diameter, from, to, "");
    }

    /** Whether the solder mask is left open over it on a side. */
    public boolean opensMaskOn(String side) {
        return PcbVocabulary.VIA_OPEN_BOTH.equals(open) || side.equals(open);
    }

}
