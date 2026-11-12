package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.PcbVocabulary;

/**
 * Where a footprint prints its part's reference or value, from a product that places a text by a point on its letters
 * with Y upwards — Altium and Eagle by where they begin, LibrePCB and KiCad as their alignment says —, while a field
 * stands on its point by its middle across the letters, Y downwards: the point is moved half their height from their bottom
 * or their top, turned with them. Along the letters a field keeps the product's point and says which part of them stands
 * on it ({@code anchor}): how far they run depends on the part placed — {@code R1} or {@code R100} —, so no point along
 * them can be worked out in advance.
 */
public final class ImportedFields {

    private ImportedFields() {
    }

    /** Where a text's point stands across its letters. */
    public enum Across {
        BOTTOM(0.5), MIDDLE(0), TOP(-0.5);

        /** How far the middle stands above the point, in the letters' heights. */
        private final double toMiddle;

        Across(double toMiddle) {
            this.toMiddle = toMiddle;
        }
    }

    /** A field from a text placed by where its letters begin, on their baseline: Altium's, Eagle's. */
    public static FootprintLines.Field begunAt(String subject, double x, double y, double height, double rotation, String layer) {
        return placedAt(subject, x, y, height, rotation, Across.BOTTOM, PcbVocabulary.TEXT_START, layer);
    }

    /**
     * @param x        where the text's point stands, in millimetres, Y upwards
     * @param height   the letters' height, in millimetres
     * @param rotation degrees, counter-clockwise
     * @param anchor   which part of the letters stands on the point along them: {@code start}, {@code middle} or {@code end}
     * @param layer    what the field is printed on, {@code silkscreen} or {@code fabrication}; null for where a field is
     *                 printed when none is named
     */
    public static FootprintLines.Field placedAt(String subject, double x, double y, double height, double rotation, Across across, String anchor,
                                                String layer) {
        RoundedPath round = new RoundedPath(FootprintOutlines.PLACES);
        double turn = Math.toRadians(rotation);
        double raised = across.toMiddle * height;
        double middleX = x - Math.sin(turn) * raised;
        double middleY = -(y + Math.cos(turn) * raised);
        return new FootprintLines.Field(subject, round.rounded(middleX), round.rounded(middleY), round.rounded(height), round.rounded(rotation),
                layer, PcbVocabulary.TEXT_MIDDLE.equals(anchor) ? null : anchor);
    }
}
