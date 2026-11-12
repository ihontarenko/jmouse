package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.exchange.imported.SheetTracing.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A pcb's edge as another product draws it: loose lines and arcs in any order, each running either way. They are chained
 * end to end into closed loops of one path.
 */
public final class OutlineStretches {

    /**
     * One line, arc or curve of an edge.
     *
     * @param through a point of it between its ends, when it is an arc
     * @param handles a cubic curve's two handles, from its start's to its end's; none for a line or an arc
     */
    public record Stretch(Point from, Point to, Optional<Point> through, List<Point> handles) {

        public Stretch {
            handles = List.copyOf(handles);
        }

        /** A line, or an arc through a point. */
        public Stretch(Point from, Point to, Optional<Point> through) {
            this(from, to, through, List.of());
        }

        /** A cubic curve, pulled by its two handles. */
        public static Stretch curve(Point from, Point first, Point second, Point to) {
            return new Stretch(from, to, Optional.empty(), List.of(first, second));
        }

        private Stretch reversed() {
            return new Stretch(to, from, through, handles.reversed());
        }

        private void drawInto(RoundedPath path) {
            if (handles.size() == 2) {
                path.curveFrom(from.x(), from.y(), handles.get(0).x(), handles.get(0).y(), handles.get(1).x(), handles.get(1).y(), to.x(), to.y());
                return;
            }
            through.ifPresentOrElse(middle -> path.arcThrough(from.x(), from.y(), middle.x(), middle.y(), to.x(), to.y()),
                    () -> path.lineFrom(from.x(), from.y(), to.x(), to.y()));
        }
    }

    private OutlineStretches() {
    }

    /**
     * Every stretch drawn into the path, each loop closed.
     *
     * @param open told where a chain ends before it closes; what it holds is drawn as it is
     */
    public static void draw(List<Stretch> stretches, RoundedPath path, Consumer<Point> open) {
        List<Stretch> remaining = new ArrayList<>(stretches);
        while (!remaining.isEmpty()) {
            Stretch first = remaining.removeFirst();
            first.drawInto(path);
            Point end = first.to();
            while (!end.sameAs(first.from())) {
                Optional<Stretch> next = nextFrom(remaining, end);
                if (next.isEmpty()) {
                    open.accept(end);
                    break;
                }
                Stretch stretch = next.get();
                remaining.remove(stretch);
                Stretch forward = stretch.from().sameAs(end) ? stretch : stretch.reversed();
                forward.drawInto(path);
                end = forward.to();
            }
            if (end.sameAs(first.from())) {
                path.close();
            }
        }
    }

    private static Optional<Stretch> nextFrom(List<Stretch> stretches, Point end) {
        return stretches.stream().filter(stretch -> stretch.from().sameAs(end) || stretch.to().sameAs(end)).findFirst();
    }
}
