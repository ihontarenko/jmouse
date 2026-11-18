package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbVocabulary;

/**
 * Words printed on the pcb.
 *
 * @param layer    {@code top-silkscreen} or {@code bottom-silkscreen}
 * @param text     the words
 * @param x        where they stand, millimetres from the left
 * @param y        where they stand, millimetres down
 * @param size     how tall their letters are, millimetres
 * @param rotation how far they are turned, degrees
 * @param anchor   which part of the words stands on their point: {@code start}, {@code middle} — the usual — or {@code end}
 */
public record Lettering(String layer, String text, double x, double y, double size, double rotation, String anchor) {

    /** Words standing round their point. */
    public Lettering(String layer, String text, double x, double y, double size, double rotation) {
        this(layer, text, x, y, size, rotation, PcbVocabulary.TEXT_MIDDLE);
    }

    public Lettering {
        anchor = anchor == null ? PcbVocabulary.TEXT_MIDDLE : anchor;
    }
}
