package org.jmouse.cad.plugin.drawings;

import org.jmouse.strokefont.StrokeFont;
import org.jmouse.strokefont.StrokeText;

/** How wide words stand on paper, in whichever lettering the document is set: what a layout must know of it. */
interface SheetLettering {

    /** @param height how high a capital stands, in millimetres */
    double widthOf(String text, double height);

    /** The engine's own stroke font, which is always there. */
    static SheetLettering strokes(StrokeFont font, int curveSteps) {
        return (text, height) -> StrokeText.lay(font, text, height, StrokeText.Anchor.START, StrokeText.Baseline.ALPHABETIC, curveSteps).width();
    }
}
