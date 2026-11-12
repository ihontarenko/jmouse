package org.jmouse.cad.panel;

/**
 * What a panel takes where its lines say nothing, in millimetres.
 *
 * @param rail             how wide a frame's rail is
 * @param boardGap         between boards that are routed apart, and between them and the rails
 * @param fiducialDiameter a fiducial's copper
 * @param fiducialOpening  the opening in the mask round it
 * @param toolingDrill     a tooling hole
 * @param tabWidth         how wide a mouse-bite tab is
 * @param tabSpacing       how much of a board's side one tab holds
 * @param biteDrill        a hole of a tab's row
 * @param bitePitch        between the holes of a row
 */
public record PanelDefaults(double rail, double boardGap, double fiducialDiameter, double fiducialOpening, double toolingDrill,
                            double tabWidth, double tabSpacing, double biteDrill, double bitePitch) {
}
