package org.jmouse.kicad;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.kicad.KiCadKeyword.END;
import static org.jmouse.kicad.KiCadKeyword.MIDDLE;
import static org.jmouse.kicad.KiCadKeyword.POINT;
import static org.jmouse.kicad.KiCadKeyword.START;
import static org.jmouse.kicad.KiCadKeyword.TRACK_ARC;

/**
 * A closed outline as KiCad writes one in its {@code (pts …)}: corners — {@code (xy x y)} — and, since KiCad 7, arcs between them —
 * {@code (arc (start …) (mid …) (end …))} —, in order. A zone with rounded corners is written so, and one whose every corner is
 * rounded has no {@code xy} at all: read for its corners alone, it would be no outline.
 *
 * @param steps its corners and arcs, in the order it runs
 */
public record KiCadOutline(List<Step> steps) {

    public KiCadOutline {
        steps = List.copyOf(steps);
    }

    /** A step of an outline: to a corner, or along an arc through its middle. */
    public sealed interface Step {

        /** Where the step ends. */
        KiCadPoint end();
    }

    /**
     * A corner of the outline.
     *
     * @param end where it stands
     */
    public record Corner(KiCadPoint end) implements Step {
    }

    /**
     * An arc of the outline, from its start through its middle to its end.
     *
     * @param start  where it begins
     * @param middle a point it passes halfway along
     * @param end    where it ends
     */
    public record Bend(KiCadPoint start, KiCadPoint middle, KiCadPoint end) implements Step {
    }

    /** The outline a {@code (pts …)} list writes. */
    public static KiCadOutline of(SExpressionList points) {
        List<Step> steps = new ArrayList<>();
        for (SExpressionList item : points.lists()) {
            if (item.is(POINT)) {
                steps.add(new Corner(KiCadPoint.of(item)));
            } else if (item.is(TRACK_ARC)) {
                steps.add(new Bend(KiCadPoint.of(item.requireList(START)), KiCadPoint.of(item.requireList(MIDDLE)), KiCadPoint.of(item.requireList(END))));
            }
        }
        return new KiCadOutline(steps);
    }

    /** Whether it has nothing of an outline: no step at all. */
    public boolean isEmpty() {
        return steps.isEmpty();
    }

    /** Where it starts: the first corner, or the start of the arc it opens with. */
    public KiCadPoint start() {
        return switch (steps.getFirst()) {
            case Corner corner -> corner.end();
            case Bend bend -> bend.start();
        };
    }
}
