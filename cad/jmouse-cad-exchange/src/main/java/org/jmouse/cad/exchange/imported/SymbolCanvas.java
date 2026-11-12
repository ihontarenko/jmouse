package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.document.PathTextNode;
import org.jmouse.cad.schematic.DrawingGrammar;
import org.jmouse.el.grammar.AssignedNode;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a symbol converted from another product is drawn: its shapes as paths — stroked, filled solid, or a body —, its
 * pins and the labels it writes its name and value at, every point in drawing units with Y downwards, rounded to a
 * hundredth and drawn from the origin given, and how far the drawing reaches. Its lines are the symbol document's
 * drawing; what a product's own shapes are is that product's import's.
 */
public final class SymbolCanvas {

    /**
     * The scale of a sheet: a 1.27 mm grid — KiCad's, and half of Eagle's 0.1 inch — is twenty drawing units, so a pin
     * on another product's grid is a pin on ours, and a body comes out the size of the catalogue's own.
     */
    public static final double UNITS_PER_MILLIMETRE = 20 / 1.27;

    /** The radius of the bubble an inverted pin carries at the body, drawing units: half a 1.27 mm grid. */
    public static final double BUBBLE = 0.635 * UNITS_PER_MILLIMETRE;

    /** The grid a symbol's size is rounded up to, and its centre to. */
    public static final double GRID = 20;

    /** A coordinate to a hundredth of a drawing unit. */
    private static final int PLACES = 2;

    /** What a shape filled with the line's colour is filled with: the drawing's ink. */
    private static final String INK = "signal";

    /** What a body is filled with: the body fill of the drawing theme, its outline stroked over it. */
    private static final String BODY_FILL = "body";

    private static final String FILL = "fill";
    private static final String PATH = "path";

    /** How a shape is drawn. */
    public enum Fill {
        /** Stroked, with every other line of the symbol. */
        STROKE,
        /** Filled with the drawing's ink: an arrow head, a dot. */
        SOLID,
        /** Filled as the part's body, its outline stroked. */
        BODY
    }

    private final RoundedPath       strokes = new RoundedPath(PLACES);
    private final List<RoundedPath> fills   = new ArrayList<>();
    private final List<RoundedPath> bodies  = new ArrayList<>();
    private final List<ClauseLineNode> pins   = new ArrayList<>();
    private final List<ClauseLineNode> labels = new ArrayList<>();
    private final double originX;
    private final double originY;
    private double reachX;
    private double reachY;
    private double minimumX = Double.POSITIVE_INFINITY;
    private double maximumX = Double.NEGATIVE_INFINITY;
    private double minimumY = Double.POSITIVE_INFINITY;
    private double maximumY = Double.NEGATIVE_INFINITY;

    /** @param originX where the drawing's origin stands in the product's frame, in drawing units: every point is drawn from it */
    public SymbolCanvas(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /** A point given in drawing units from the product's origin, Y downwards, as the drawing has it; the reach grows to hold it. */
    public double[] point(double x, double y) {
        double[] drawn = {strokes.rounded(x - originX), strokes.rounded(y - originY)};
        reach(drawn[0], drawn[1]);
        return drawn;
    }

    /** The drawing reaches this point, already the drawing's: a circle's edge. */
    public void reach(double x, double y) {
        reachX = Math.max(reachX, Math.abs(x));
        reachY = Math.max(reachY, Math.abs(y));
        minimumX = Math.min(minimumX, x);
        maximumX = Math.max(maximumX, x);
        minimumY = Math.min(minimumY, y);
        maximumY = Math.max(maximumY, y);
    }

    /** The path a shape of this fill is drawn into: the strokes, or a fill or a body of its own. */
    public RoundedPath path(Fill fill) {
        return switch (fill) {
            case SOLID -> {
                fills.add(new RoundedPath(PLACES));
                yield fills.getLast();
            }
            case BODY -> {
                bodies.add(new RoundedPath(PLACES));
                yield bodies.getLast();
            }
            case STROKE -> strokes;
        };
    }

    /**
     * A pin's line, from where it connects to where it meets the body, both the drawing's; an inverted one stops short of
     * the body with a {@link #BUBBLE} against it.
     */
    public void pinLine(double[] at, double[] body, boolean inverted) {
        if (!inverted) {
            strokes.moveTo(at[0], at[1]).lineTo(body[0], body[1]);
            return;
        }
        double length = Math.hypot(body[0] - at[0], body[1] - at[1]);
        double dx = length == 0 ? 0 : (body[0] - at[0]) / length;
        double dy = length == 0 ? 0 : (body[1] - at[1]) / length;
        strokes.moveTo(at[0], at[1]).lineTo(body[0] - dx * 2 * BUBBLE, body[1] - dy * 2 * BUBBLE);
        strokes.circle(body[0] - dx * BUBBLE, body[1] - dy * BUBBLE, BUBBLE);
    }

    /**
     * A pin by the name the part connects it by, where it connects, where its lead reaches the body, or null for no lead of
     * its own, and the name drawn beside it, or null for none.
     */
    public void pin(String name, double[] at, double[] body, String shown) {
        LineBuilder line = LineBuilder.line(DrawingGrammar.PIN).slot("name", Nodes.name(name))
                .clause("at", Nodes.point(at[0], at[1]));
        if (body != null) {
            line.clause("to", Nodes.point(body[0], body[1]));
        }
        if (shown != null) {
            line.ending(Nodes.text(shown));
        }
        pins.add(line.build());
    }

    /** Where the part's {@code subject} — its {@code name}, its {@code value} — is written. */
    public void label(String subject, double[] at) {
        labels.add(LineBuilder.line(DrawingGrammar.LABEL).slot("subject", Nodes.word(subject))
                .clause("at", Nodes.point(at[0], at[1])).build());
    }

    /** The middle of what was drawn, on the grid; the origin when nothing was. */
    public double[] centre() {
        if (minimumX > maximumX) {
            return new double[] {0, 0};
        }
        return new double[] {Math.round((minimumX + maximumX) / 2 / GRID) * GRID,
                Math.round((minimumY + maximumY) / 2 / GRID) * GRID};
    }

    /**
     * The drawing's lines: its size round its reach on the grid, its strokes, its pins and labels, then its bodies under
     * what is filled solid — a body is the ground a part's marks stand on.
     */
    public List<Expression> lines() {
        List<Expression> lines = new ArrayList<>();
        lines.add(LineBuilder.line(DrawingGrammar.SIZE).slot("extent", Nodes.extent(width(), height())).build());
        if (!strokes.isEmpty()) {
            lines.add(element(null, strokes, null));
        }
        lines.addAll(pins);
        lines.addAll(labels);
        for (int index = 0; index < bodies.size(); index++) {
            lines.add(element("body_" + (index + 1), bodies.get(index), BODY_FILL));
        }
        for (int index = 0; index < fills.size(); index++) {
            lines.add(element("filled_" + (index + 1), fills.get(index), INK));
        }
        return lines;
    }

    private double width() {
        return Math.max(GRID, Math.ceil(2 * reachX / GRID) * GRID);
    }

    private double height() {
        return Math.max(GRID, Math.ceil(2 * reachY / GRID) * GRID);
    }

    /** {@code path [name] "data" [{ fill = … }]}. */
    private static ClauseBlockNode element(String name, RoundedPath path, String fill) {
        LineBuilder head = LineBuilder.line(DrawingGrammar.element(PATH));
        if (name != null) {
            head.slot("name", Nodes.name(name));
        }
        ClauseBlockNode element = new ClauseBlockNode(PATH, head.ending(new PathTextNode(path.build())).build());
        if (fill != null) {
            element.open();
            element.addExpression(LineBuilder.line(DrawingGrammar.PROPERTY).slot("name", Nodes.name(FILL))
                    .slot("value", new AssignedNode(Nodes.formula(Nodes.word(fill)))).build());
        }
        return element;
    }
}
