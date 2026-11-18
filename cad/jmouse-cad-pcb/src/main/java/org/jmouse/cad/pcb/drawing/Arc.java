package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.drawing.Arrangement;

/**
 * A track bent as an arc, through three points.
 *
 * @param layer       the copper layer it is on
 * @param width       how wide it is, millimetres
 * @param from        where it starts
 * @param through     a point it bends through, between its ends
 * @param to          where it ends
 * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
 * @param finish      what its copper is left as: bare, the solder mask open along it; empty for under the mask
 */
public record Arc(String layer, double width, Point from, Point through, Point to, Arrangement arrangement, String finish) {

    public Arc {
        arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        finish = finish == null ? "" : finish;
    }

    /** An arc in no group, free, under the mask. */
    public Arc(String layer, double width, Point from, Point through, Point to) {
        this(layer, width, from, through, to, Arrangement.FREE, "");
    }

    /** The same arc standing otherwise among the others. */
    public Arc arranged(Arrangement other) {
        return new Arc(layer, width, from, through, to, other, finish);
    }

    public boolean bare() {
        return PcbVocabulary.FINISH_BARE.equals(finish);
    }
}
