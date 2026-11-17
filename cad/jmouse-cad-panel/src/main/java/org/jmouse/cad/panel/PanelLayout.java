package org.jmouse.cad.panel;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.svg.path.node.PathBuilder;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Where everything on a panel stands: each copy of each board, the frame, what separates the boards, and the material they all
 * make, whose outline is the flattened pcb's edge. A panel that would fall apart is refused here, with which board.
 */
final class PanelLayout {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();
    private static final PathParser PATHS = new PathParser();

    /** How finely a board's curved edge is stepped for the material's outline. */
    private static final int CURVE_STEPS = 24;

    /** How far two boards' edges may differ and still be one straight score line. */
    private static final double SAME_LINE = 1e-6;

    /** How far a tab reaches into a board, so that the two are one piece of material. */
    private static final double TAB_OVERLAP = 0.01;

    /** A score line's pen, on the documentation layer. */
    private static final double SCORE_WIDTH = 0.1;

    private static final String DOCUMENTATION = "documentation";
    private static final String MASK = "mask";

    private final List<Copy> copies = new ArrayList<>();
    private final List<PcbDrawing.Hole> holes = new ArrayList<>();
    private final List<PcbDrawing.Shape> shapes = new ArrayList<>();
    private final List<PcbDrawing.Text> texts = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final List<Geometry> material = new ArrayList<>();
    private final PanelDefaults defaults;
    private String outline = "";
    private Envelope extent = new Envelope();

    /** One copy of a board where it stands. */
    record Copy(int number, String pcb, BoardMotion motion, Envelope bounds, Geometry area) {
    }

    private PanelLayout(PanelDefaults defaults) {
        this.defaults = defaults;
    }

    static PanelLayout of(PanelDrawing panel, Map<String, PcbDrawing> drawings, PanelDefaults defaults) {
        PanelLayout layout = new PanelLayout(defaults);
        layout.placeBoards(panel, drawings);
        layout.requireNoOverlap();
        Envelope boards = layout.boardsExtent();
        double clearance = panel.separation() == PanelSeparation.MOUSE_BITES ? defaults.boardGap() : 0;
        Envelope inner = new Envelope(boards);
        Envelope outer = new Envelope(boards);
        panel.frame().ifPresent(frame -> {
            inner.expandBy(clearance);
            outer.init(layout.frameOf(frame, inner));
        });
        layout.extent = outer;
        switch (panel.separation()) {
            case V_SCORE -> layout.score(outer);
            case MOUSE_BITES -> layout.bite(panel, inner);
            case NONE -> {
                // Nothing to draw: the factory is told by the boards' own edges.
            }
        }
        panel.fiducials().forEach(fiducial -> layout.fiducial(fiducial.at(), fiducial.diameter().orElse(defaults.fiducialDiameter()),
                fiducial.opening().orElse(defaults.fiducialOpening())));
        panel.tooling().forEach(tooling -> layout.holes.add(new PcbDrawing.Hole(tooling.at().x(), tooling.at().y(),
                tooling.drill().orElse(defaults.toolingDrill()), 0)));
        layout.closeOutline();
        return layout;
    }

    List<Copy> copies() {
        return copies;
    }

    String outline() {
        return outline;
    }

    Envelope extent() {
        return extent;
    }

    List<PcbDrawing.Hole> holes() {
        return holes;
    }

    List<PcbDrawing.Shape> shapes() {
        return shapes;
    }

    List<PcbDrawing.Text> texts() {
        return texts;
    }

    List<String> notes() {
        return notes;
    }

    private void placeBoards(PanelDrawing panel, Map<String, PcbDrawing> drawings) {
        int number = 1;
        for (PanelDrawing.Board board : panel.boards()) {
            if (board.rotation() % 90 != 0) {
                throw new IllegalArgumentException("'" + board.pcb() + "' stands turned by " + board.rotation() + "°: a board on a panel turns a quarter at a time.");
            }
            if (board.across() < 1 || board.down() < 1) {
                throw new IllegalArgumentException("'" + board.pcb() + "' stands " + board.across() + " across and " + board.down() + " down: at least one of each.");
            }
            Geometry own = areaOf(board.pcb(), drawings.get(board.pcb()));
            BoardMotion turned = new BoardMotion(board.rotation(), 0, 0);
            Envelope bounds = moved(own, turned).getEnvelopeInternal();
            double gap = board.gap().orElse(panel.separation() == PanelSeparation.V_SCORE ? 0 : defaults.boardGap());
            for (int down = 0; down < board.down(); down++) {
                for (int across = 0; across < board.across(); across++) {
                    double x = board.x() + across * (bounds.getWidth() + gap);
                    double y = board.y() + down * (bounds.getHeight() + gap);
                    BoardMotion motion = new BoardMotion(board.rotation(), x - bounds.getMinX(), y - bounds.getMinY());
                    Geometry area = moved(own, motion);
                    copies.add(new Copy(number, board.pcb(), motion, area.getEnvelopeInternal(), area));
                    material.add(area);
                    number++;
                }
            }
        }
    }

    /** A pcb's area by its edge: the largest run its outside, those within it cut out. */
    private static Geometry areaOf(String slug, PcbDrawing pcb) {
        if (pcb.outline() == null || pcb.outline().isBlank()) {
            throw new IllegalArgumentException("'" + slug + "' has no edge, so it cannot stand on a panel.");
        }
        List<Polygon> runs = new ArrayList<>();
        for (List<double[]> run : PathPolylines.of(PATHS.parse(pcb.outline()), CURVE_STEPS)) {
            if (run.size() >= 3) {
                Coordinate[] ring = new Coordinate[run.size() + (closed(run) ? 0 : 1)];
                for (int index = 0; index < run.size(); index++) {
                    ring[index] = new Coordinate(run.get(index)[0], run.get(index)[1]);
                }
                ring[ring.length - 1] = new Coordinate(ring[0]);
                runs.add(GEOMETRY.createPolygon(ring));
            }
        }
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("'" + slug + "' has an edge that closes no area, so it cannot stand on a panel.");
        }
        runs.sort(Comparator.comparingDouble(Polygon::getArea).reversed());
        Geometry area = runs.getFirst().buffer(0);
        for (Polygon cutout : runs.subList(1, runs.size())) {
            area = Areas.difference(area, cutout.buffer(0));
        }
        return area;
    }

    private static boolean closed(List<double[]> run) {
        double[] first = run.getFirst();
        double[] last = run.getLast();
        return first[0] == last[0] && first[1] == last[1];
    }

    private static Geometry moved(Geometry area, BoardMotion motion) {
        Geometry copy = area.copy();
        copy.apply((Coordinate coordinate) -> {
            PcbDrawing.Point moved = motion.point(new PcbDrawing.Point(coordinate.x, coordinate.y));
            coordinate.x = moved.x();
            coordinate.y = moved.y();
        });
        copy.geometryChanged();
        return copy;
    }

    private void requireNoOverlap() {
        for (int first = 0; first < copies.size(); first++) {
            for (int second = first + 1; second < copies.size(); second++) {
                Copy one = copies.get(first);
                Copy other = copies.get(second);
                if (Areas.common(one.area(), other.area()).getArea() > SAME_LINE) {
                    throw new IllegalArgumentException("'" + one.pcb() + "' and '" + other.pcb() + "' stand on one another on the panel.");
                }
            }
        }
    }

    private Envelope boardsExtent() {
        Envelope extentOfBoards = new Envelope();
        copies.forEach(copy -> extentOfBoards.expandToInclude(copy.bounds()));
        return extentOfBoards;
    }

    /** The rails round what they hold, the frame's own fiducials and tooling holes on them; what they all come to. */
    private Envelope frameOf(PanelDrawing.Frame frame, Envelope inner) {
        double rail = frame.rail().orElse(defaults.rail());
        boolean top = frame.sides().contains(PanelSide.TOP);
        boolean bottom = frame.sides().contains(PanelSide.BOTTOM);
        boolean left = frame.sides().contains(PanelSide.LEFT);
        boolean right = frame.sides().contains(PanelSide.RIGHT);
        Envelope outer = new Envelope(inner.getMinX() - (left ? rail : 0), inner.getMaxX() + (right ? rail : 0),
                inner.getMinY() - (top ? rail : 0), inner.getMaxY() + (bottom ? rail : 0));
        if (top) {
            material.add(rectangle(outer.getMinX(), outer.getMaxX(), outer.getMinY(), inner.getMinY()));
        }
        if (bottom) {
            material.add(rectangle(outer.getMinX(), outer.getMaxX(), inner.getMaxY(), outer.getMaxY()));
        }
        if (left) {
            material.add(rectangle(outer.getMinX(), inner.getMinX(), outer.getMinY(), outer.getMaxY()));
        }
        if (right) {
            material.add(rectangle(inner.getMaxX(), outer.getMaxX(), outer.getMinY(), outer.getMaxY()));
        }
        List<PcbDrawing.Point> corners = cornersOnRails(outer, rail, top, bottom, left, right);
        frame.tooling().ifPresent(count -> {
            for (PcbDrawing.Point corner : upTo(corners, count)) {
                holes.add(new PcbDrawing.Hole(corner.x(), corner.y(), defaults.toolingDrill(), 0));
            }
        });
        frame.fiducials().ifPresent(count -> {
            double along = defaults.toolingDrill() + defaults.fiducialOpening();
            for (PcbDrawing.Point corner : upTo(corners, count)) {
                boolean alongX = top || bottom;
                double direction = alongX ? Math.signum(outer.centre().x - corner.x()) : Math.signum(outer.centre().y - corner.y());
                fiducial(alongX ? new PcbDrawing.Point(corner.x() + direction * along, corner.y())
                                : new PcbDrawing.Point(corner.x(), corner.y() + direction * along),
                        defaults.fiducialDiameter(), defaults.fiducialOpening());
            }
        });
        return outer;
    }

    /** The middle of a rail's width at each corner of the panel, as far as there are rails. */
    private static List<PcbDrawing.Point> cornersOnRails(Envelope outer, double rail, boolean top, boolean bottom, boolean left, boolean right) {
        double half = rail / 2;
        List<PcbDrawing.Point> corners = new ArrayList<>();
        if (top || left) {
            corners.add(new PcbDrawing.Point(outer.getMinX() + half, outer.getMinY() + half));
        }
        if (top || right) {
            corners.add(new PcbDrawing.Point(outer.getMaxX() - half, outer.getMinY() + half));
        }
        if (bottom || left) {
            corners.add(new PcbDrawing.Point(outer.getMinX() + half, outer.getMaxY() - half));
        }
        if (bottom || right) {
            corners.add(new PcbDrawing.Point(outer.getMaxX() - half, outer.getMaxY() - half));
        }
        return corners;
    }

    private List<PcbDrawing.Point> upTo(List<PcbDrawing.Point> corners, int count) {
        if (count > corners.size()) {
            notes.add("The frame has room for " + corners.size() + " at its corners, not " + count + ".");
        }
        return corners.subList(0, Math.min(count, corners.size()));
    }

    /** A fiducial on both faces: bare copper in an opening of the mask. */
    private void fiducial(PcbDrawing.Point at, double diameter, double opening) {
        for (String side : List.of(PcbDrawing.TOP, PcbDrawing.BOTTOM)) {
            shapes.add(new PcbDrawing.Shape(side, 0, true, "", circle(at, diameter / 2)));
            shapes.add(new PcbDrawing.Shape(PcbDrawing.layerOf(side, MASK), 0, true, "", circle(at, opening / 2)));
        }
    }

    private static String circle(PcbDrawing.Point at, double radius) {
        return PathBuilder.path().moveTo(at.x() - radius, at.y()).arcTo(radius, radius, 0, true, true, at.x() + radius, at.y())
                .arcTo(radius, radius, 0, true, true, at.x() - radius, at.y()).close().build().toSource();
    }

    /** A line along every board's edges, right across the panel; each must miss every board's inside. */
    private void score(Envelope outer) {
        TreeSet<Double> across = new TreeSet<>();
        TreeSet<Double> down = new TreeSet<>();
        for (Copy copy : copies) {
            addLine(across, copy.bounds().getMinX());
            addLine(across, copy.bounds().getMaxX());
            addLine(down, copy.bounds().getMinY());
            addLine(down, copy.bounds().getMaxY());
        }
        for (double x : across) {
            requireMisses(x, true);
            shapes.add(scoreLine(x, outer.getMinY(), x, outer.getMaxY()));
        }
        for (double y : down) {
            requireMisses(y, false);
            shapes.add(scoreLine(outer.getMinX(), y, outer.getMaxX(), y));
        }
        for (Copy copy : copies) {
            if (Math.abs(copy.area().getArea() - copy.bounds().getArea()) > SAME_LINE * copy.bounds().getArea()) {
                throw new IllegalArgumentException("'" + copy.pcb() + "' is not a rectangle: V-score cuts straight across, so it needs mouse bites.");
            }
        }
        notes.add("V-score lines are on the documentation layer.");
    }

    private static void addLine(TreeSet<Double> lines, double at) {
        Double near = lines.floor(at + SAME_LINE);
        if (near == null || Math.abs(near - at) > SAME_LINE) {
            lines.add(at);
        }
    }

    private void requireMisses(double at, boolean vertical) {
        for (Copy copy : copies) {
            double low = vertical ? copy.bounds().getMinX() : copy.bounds().getMinY();
            double high = vertical ? copy.bounds().getMaxX() : copy.bounds().getMaxY();
            if (at > low + SAME_LINE && at < high - SAME_LINE) {
                throw new IllegalArgumentException("A V-score line along another board's edge would cut '" + copy.pcb()
                                                   + "': boards under V-score stand in straight rows and columns.");
            }
        }
    }

    private static PcbDrawing.Shape scoreLine(double fromX, double fromY, double toX, double toY) {
        return new PcbDrawing.Shape(DOCUMENTATION, SCORE_WIDTH, false, "",
                PathBuilder.path().moveTo(fromX, fromY).lineTo(toX, toY).build().toSource());
    }

    /** Tabs from each board across its channel, to the next board or to the rail, each with its row of holes on the board's edge. */
    private void bite(PanelDrawing panel, Envelope inner) {
        for (int index = 0; index < copies.size(); index++) {
            Copy copy = copies.get(index);
            Envelope bounds = copy.bounds();
            if (panel.frame().isPresent()) {
                biteToward(copy, bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), inner.getMinY(), true);
                biteToward(copy, bounds.getMinX(), bounds.getMaxX(), bounds.getMaxY(), inner.getMaxY(), true);
                biteToward(copy, bounds.getMinY(), bounds.getMaxY(), bounds.getMinX(), inner.getMinX(), false);
                biteToward(copy, bounds.getMinY(), bounds.getMaxY(), bounds.getMaxX(), inner.getMaxX(), false);
            }
            for (int later = index + 1; later < copies.size(); later++) {
                biteBetween(copy, copies.get(later));
            }
        }
    }

    private void biteToward(Copy copy, double spanFrom, double spanTo, double edge, double facing, boolean horizontalEdge) {
        // A rail with another board nearer than it is reached through that board, not past it.
        if (Math.abs(edge - facing) < SAME_LINE || standsBetween(copy, spanFrom, spanTo, edge, facing, horizontalEdge)) {
            return;
        }
        tabs(copy, spanFrom, spanTo, edge, facing, horizontalEdge);
    }

    private boolean standsBetween(Copy copy, double spanFrom, double spanTo, double edge, double facing, boolean horizontalEdge) {
        double low = Math.min(edge, facing);
        double high = Math.max(edge, facing);
        for (Copy other : copies) {
            if (other == copy) {
                continue;
            }
            Envelope bounds = other.bounds();
            double otherLow = horizontalEdge ? bounds.getMinY() : bounds.getMinX();
            double otherHigh = horizontalEdge ? bounds.getMaxY() : bounds.getMaxX();
            double otherFrom = horizontalEdge ? bounds.getMinX() : bounds.getMinY();
            double otherTo = horizontalEdge ? bounds.getMaxX() : bounds.getMaxY();
            if (otherHigh > low + SAME_LINE && otherLow < high - SAME_LINE && otherTo > spanFrom + SAME_LINE && otherFrom < spanTo - SAME_LINE) {
                return true;
            }
        }
        return false;
    }

    private void biteBetween(Copy one, Copy other) {
        Envelope a = one.bounds();
        Envelope b = other.bounds();
        double spanFromX = Math.max(a.getMinX(), b.getMinX());
        double spanToX = Math.min(a.getMaxX(), b.getMaxX());
        double spanFromY = Math.max(a.getMinY(), b.getMinY());
        double spanToY = Math.min(a.getMaxY(), b.getMaxY());
        if (spanToX > spanFromX && a.getMaxY() <= b.getMinY()) {
            tabs(one, spanFromX, spanToX, a.getMaxY(), b.getMinY(), true);
        } else if (spanToX > spanFromX && b.getMaxY() <= a.getMinY()) {
            tabs(other, spanFromX, spanToX, b.getMaxY(), a.getMinY(), true);
        } else if (spanToY > spanFromY && a.getMaxX() <= b.getMinX()) {
            tabs(one, spanFromY, spanToY, a.getMaxX(), b.getMinX(), false);
        } else if (spanToY > spanFromY && b.getMaxX() <= a.getMinX()) {
            tabs(other, spanFromY, spanToY, b.getMaxX(), a.getMinX(), false);
        }
    }

    private void tabs(Copy copy, double spanFrom, double spanTo, double edge, double facing, boolean horizontalEdge) {
        double span = spanTo - spanFrom;
        int count = Math.max(1, (int) Math.floor(span / defaults.tabSpacing()));
        for (int tab = 0; tab < count; tab++) {
            double centre = spanFrom + span * (tab + 0.5) / count;
            double half = Math.min(defaults.tabWidth(), span) / 2;
            // A board's real edge may stand back from its bounds: the tab goes on until it meets it.
            double reached = reachOf(copy, centre, edge, facing, horizontalEdge);
            double direction = Math.signum(facing - edge);
            double into = reached - direction * TAB_OVERLAP;
            material.add(horizontalEdge ? rectangle(centre - half, centre + half, Math.min(into, facing), Math.max(into, facing))
                                        : rectangle(Math.min(into, facing), Math.max(into, facing), centre - half, centre + half));
            for (double along = centre - half + defaults.bitePitch() / 2; along < centre + half; along += defaults.bitePitch()) {
                holes.add(horizontalEdge ? new PcbDrawing.Hole(along, reached, defaults.biteDrill(), 0)
                                         : new PcbDrawing.Hole(reached, along, defaults.biteDrill(), 0));
            }
        }
    }

    /** Where a line from the facing edge, across the channel into the board, first meets the board. */
    private double reachOf(Copy copy, double centre, double edge, double facing, boolean horizontalEdge) {
        double far = horizontalEdge ? (facing > edge ? copy.bounds().getMinY() : copy.bounds().getMaxY())
                                    : (facing > edge ? copy.bounds().getMinX() : copy.bounds().getMaxX());
        LineString across = horizontalEdge
                ? GEOMETRY.createLineString(new Coordinate[]{new Coordinate(centre, facing), new Coordinate(centre, far)})
                : GEOMETRY.createLineString(new Coordinate[]{new Coordinate(facing, centre), new Coordinate(far, centre)});
        Geometry met = Areas.crossing(across, copy.area());
        if (met.isEmpty()) {
            throw new IllegalArgumentException("A tab of '" + copy.pcb() + "' meets no edge of it: move the board or set its tabs by hand.");
        }
        Envelope touched = met.getEnvelopeInternal();
        if (horizontalEdge) {
            return facing > edge ? touched.getMaxY() : touched.getMinY();
        }
        return facing > edge ? touched.getMaxX() : touched.getMinX();
    }

    private static Geometry rectangle(double minimumX, double maximumX, double minimumY, double maximumY) {
        return GEOMETRY.toGeometry(new Envelope(minimumX, maximumX, minimumY, maximumY));
    }

    /** All of it as one piece of material, its boundary the panel's edge; several pieces would fall apart, and are refused. */
    private void closeOutline() {
        Geometry whole = Areas.unionOf(material);
        if (whole.getNumGeometries() > 1) {
            throw new IllegalArgumentException("The panel comes to " + whole.getNumGeometries() + " separate pieces: join its boards with a frame or a separation.");
        }
        Polygon polygon = (Polygon) whole.getGeometryN(0);
        PathBuilder path = PathBuilder.path();
        ring(path, polygon.getExteriorRing());
        for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++) {
            ring(path, polygon.getInteriorRingN(hole));
        }
        outline = path.build().toSource();
        if (extent.isNull() || !extent.contains(polygon.getEnvelopeInternal())) {
            extent = polygon.getEnvelopeInternal();
        }
    }

    private static void ring(PathBuilder path, LineString ring) {
        Coordinate[] points = ring.getCoordinates();
        path.moveTo(points[0].x, points[0].y);
        for (int index = 1; index < points.length - 1; index++) {
            path.lineTo(points[index].x, points[index].y);
        }
        path.close();
    }

    @SuppressWarnings("unused")
    private static Map<String, Integer> nothing() {
        return new LinkedHashMap<>();
    }
}
