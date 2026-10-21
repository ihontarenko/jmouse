package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;
import org.jmouse.parallax.script.Block.FootprintPad;
import org.jmouse.parallax.script.Block.PadShape;

/** A footprint's lines, as {@code FootprintReader} reads them: its pads, then its outlines, in millimetres. */
final class FootprintWriter {

    private FootprintWriter() {
    }

    /** A footprint document's body: one line a pad, one an outline. */
    static void drawing(Block.Footprint footprint, SourceLines lines) {
        footprint.pads().forEach(pad -> lines.line(pad(pad)));
        footprint.outlines().forEach(outline -> lines.line(outline(outline)));
    }

    /** {@code pad "1" through-hole rectangle at 0, 0 size 1.905 x 2 drill 1.1}. */
    static String pad(FootprintPad pad) {
        StringBuilder line = new StringBuilder("pad ").append(Values.text(pad.number())).append(' ')
                .append(pad.type().word()).append(' ').append(pad.shape().word())
                .append(" at ").append(Values.point(pad.x(), pad.y()));
        if (pad.rotation() != 0) {
            line.append(" rotate ").append(Values.number(pad.rotation()));
        }
        line.append(" size ").append(Values.number(pad.width())).append(" x ").append(Values.number(pad.height()));
        if (pad.drillWidth() > 0) {
            line.append(" drill ").append(Values.number(pad.drillWidth()));
            if (pad.drillHeight() != pad.drillWidth()) {
                line.append(" x ").append(Values.number(pad.drillHeight()));
            }
        }
        if (pad.shape() == PadShape.ROUNDED_RECTANGLE && pad.roundness() > 0) {
            line.append(" roundness ").append(Values.number(pad.roundness()));
        }
        return line.toString();
    }

    /** {@code outline silkscreen width 0.12 "M…"}. */
    static String outline(FootprintOutline outline) {
        StringBuilder line = new StringBuilder("outline ").append(outline.layer());
        if (outline.width() > 0) {
            line.append(" width ").append(Values.number(outline.width()));
        }
        if (outline.filled()) {
            line.append(" filled");
        }
        return line.append(' ').append(Values.text(outline.path())).toString();
    }
}
