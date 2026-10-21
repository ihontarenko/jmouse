package org.jmouse.kicad.footprint;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A line a footprint draws on one of its layers — the silkscreen a board is printed with, the fabrication outline of the
 * part, its courtyard —, in millimetres, Y downwards.
 */
public sealed interface KiCadFootprintGraphic {

    /** The layer it is drawn on: {@code F.SilkS}, {@code F.Fab}, {@code F.CrtYd} … */
    String layer();

    /** The stroke's width; 0 when KiCad writes none. */
    double width();

    /** Whether its inside is filled — a solid polygon, a filled rectangle. */
    boolean filled();

    record Line(KiCadPoint start, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {
    }

    record Rectangle(KiCadPoint start, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {
    }

    /** A circle through {@code end}, around {@code center}. */
    record Circle(KiCadPoint center, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {

        public double radius() {
            return Math.hypot(end.x() - center.x(), end.y() - center.y());
        }
    }

    /** An arc from {@code start} through {@code middle} to {@code end}. */
    record Arc(KiCadPoint start, KiCadPoint middle, KiCadPoint end, String layer, double width, boolean filled)
            implements KiCadFootprintGraphic {
    }

    record Polygon(List<KiCadPoint> points, String layer, double width, boolean filled) implements KiCadFootprintGraphic {
        public Polygon {
            points = List.copyOf(points);
        }
    }

    /** A footprint's graphic list, if it is one of the shapes it draws; empty for anything else (texts, dimensions). */
    static Optional<KiCadFootprintGraphic> of(KiCadList list) {
        String layer = list.wordOf(LAYER).orElse("");
        double width = list.list(STROKE).flatMap(stroke -> stroke.list(WIDTH)).map(stroked -> stroked.number(1))
                .or(() -> list.list(WIDTH).map(written -> written.number(1))).orElse(0.0);
        boolean filled = list.list(FILL).map(fill -> fill.size() > 1 && !fill.word(1).equals("none")
                && !fill.word(1).equals("no")).orElse(false);
        if (list.is(LINE)) {
            return Optional.of(new Line(point(list, START), point(list, END), layer, width, filled));
        }
        if (list.is(RECTANGLE)) {
            return Optional.of(new Rectangle(point(list, START), point(list, END), layer, width, filled));
        }
        if (list.is(CIRCLE)) {
            return Optional.of(new Circle(point(list, CENTER), point(list, END), layer, width, filled));
        }
        if (list.is(ARC) && list.list(MIDDLE).isPresent()) {
            return Optional.of(new Arc(point(list, START), point(list, MIDDLE), point(list, END), layer, width, filled));
        }
        if (list.is(POLYGON)) {
            List<KiCadPoint> points = new ArrayList<>();
            list.list(POINTS).ifPresent(written -> written.lists(POINT).forEach(xy -> points.add(KiCadPoint.of(xy))));
            return Optional.of(new Polygon(points, layer, width, filled));
        }
        return Optional.empty();
    }

    private static KiCadPoint point(KiCadList list, org.jmouse.kicad.KiCadKeyword name) {
        return KiCadPoint.of(list.requireList(name));
    }
}
