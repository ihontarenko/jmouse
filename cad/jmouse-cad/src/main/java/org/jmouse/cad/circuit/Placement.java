package org.jmouse.cad.circuit;

import org.jmouse.cad.Point;

import java.util.List;

/** Where something of a circuit is drawn on its sheet: the {@code layout} block's lines. */
public sealed interface Placement {

    String reference();

    /**
     * A part where it stands — or one block of it, placed on its own: {@code U4.B at …}.
     *
     * @param unit     the block placed on its own, {@code B}; null for the part as a whole
     * @param name     where its reference is written, from its centre, or null where the sheet puts it
     * @param value    where its value is written, from its centre, or null
     * @param readings where an instrument's readings are written, from its centre, or null
     * @param size     a placed sheet's box, as KiCad sizes a hierarchical sheet, or null for a part drawn by its symbol
     * @param shows    what an instrument's display shows — {@code rms}, {@code graph} —, as written
     */
    record PlacedPart(String reference, String unit, Point at, double rotation, boolean mirrored, Point name, Point value,
                      Point readings, Point size, List<String> shows) implements Placement {

        public PlacedPart {
            shows = List.copyOf(shows);
        }
    }

    /**
     * A mark that is no part: a junction, a ground, a common, a label, a power flag.
     *
     * @param symbol what it is — {@code junction}, {@code ground}, {@code label}… —, as written
     * @param net    the net it names — {@code label vp}, {@code ground agnd} —, or null
     */
    record Mark(String reference, String symbol, String net, Point at, double rotation)
            implements Placement {
    }

    /** A wire from one pin to another — {@code R1.negative} —, through its corners. */
    record Wire(String reference, String from, String to, List<Point> via) implements Placement {

        public Wire {
            via = List.copyOf(via);
        }
    }
}
