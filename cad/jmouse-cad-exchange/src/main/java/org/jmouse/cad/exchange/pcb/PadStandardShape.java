package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;

import java.util.Optional;

/**
 * A pad as one of the standard shapes the fabrication formats name — a circle, a rectangle, a rounded rectangle, an
 * oval —, its size as it stands on the pcb, where it is one turned a whole quarter. A pad of another shape or turn has
 * none, and a format draws it by its outline. Which of these a format has a shape of its own for is the format's.
 */
public sealed interface PadStandardShape {

    record Circle(double diameter) implements PadStandardShape {
    }

    record Rectangle(double width, double height) implements PadStandardShape {
    }

    record RoundedRectangle(double width, double height, double radius) implements PadStandardShape {
    }

    record Oval(double width, double height) implements PadStandardShape {
    }

    /**
     * The pad's standard shape where its part stands it, grown all round by {@code growth}; empty for a custom or a
     * trapezoid pad, and for a rectangle, a rounded one or an oval turned other than a whole quarter.
     */
    static Optional<PadStandardShape> of(Placement placement, FootprintDrawing.Pad pad, double growth) {
        double turn = (((pad.rotation() + placement.rotation()) % 360) + 360) % 360;
        boolean quarter = turn % 90 == 0;
        boolean across = turn == 90 || turn == 270;
        double width = (across ? pad.height() : pad.width()) + 2 * growth;
        double height = (across ? pad.width() : pad.height()) + 2 * growth;
        PadStandardShape shape = switch (pad.shape()) {
            case "circle" -> new Circle(pad.width() + 2 * growth);
            case "rectangle" -> quarter ? new Rectangle(width, height) : null;
            case "rounded-rectangle" -> quarter
                    ? new RoundedRectangle(width, height, Math.min(pad.width(), pad.height()) * pad.roundness() + growth) : null;
            case "oval" -> quarter ? new Oval(width, height) : null;
            default -> null;
        };
        return Optional.ofNullable(shape);
    }
}
