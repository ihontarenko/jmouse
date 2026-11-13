package org.jmouse.cad.panel;

import org.jmouse.settings.Label;
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
public record PanelDefaults(@Label(value = "Rail", unit = "mm") double rail,
                            @Label(value = "Board gap", unit = "mm") double boardGap,
                            @Label(value = "Fiducial diameter", unit = "mm") double fiducialDiameter,
                            @Label(value = "Fiducial opening", unit = "mm") double fiducialOpening,
                            @Label(value = "Tooling drill", unit = "mm") double toolingDrill,
                            @Label(value = "Tab width", unit = "mm") double tabWidth,
                            @Label(value = "Tab spacing", unit = "mm") double tabSpacing,
                            @Label(value = "Bite drill", unit = "mm") double biteDrill,
                            @Label(value = "Bite pitch", unit = "mm") double bitePitch) {
}
