package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.NetClass;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnState;
import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.Teardrops;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What copper must look like whatever is done to it. No single drag proves anything: each case below runs a track through many
 * pulls in many directions, round what is in the way, one after another, and every shape it comes to is held to the same rules.
 * Moved from the editor's own tests with the router on 2026-11-11: the editor asks the engine.
 */
class RoutingEngineTest {

    private static final RoutingSettings SETTINGS = read(RoutingSettings.SECTION.resource(), RoutingSettings.class);

    private static final Map<String, Double> VALUES = Map.of("clearance", 0.2, "track-width", 0.25, "least-track-width", 0.15,
            "edge-clearance", 0.3, "via-drill", 0.3, "via-diameter", 0.6, "least-drill", 0.2, "least-annular-ring", 0.1);

    private static final int CURVE_STEPS = 16;

    /** Parts moved as the editor says it: only what the reader moved — the copper they pull or push is the engine's, as rubber bands. */
    private static PartDrag drag(PcbDrawing before, PcbDrawing moved, double angleStep) {
        return new PartDrag(before, moved, angleStep, PartDragMode.RECONNECT);
    }

    /** What a release laid; a refusal fails the test, saying why. */
    private static PcbDrawing laidBy(RoutingEngine engine, PcbDrawing before, PcbDrawing moved, double angleStep) {
        return switch (engine.tracksLaid(drag(before, moved, angleStep))) {
            case RoutingEngine.Release.Laid laid -> laid.pcb();
            case RoutingEngine.Release.Refused refused -> throw new AssertionError(refused.reason());
        };
    }

    private static final PcbChecks.Settings CHECKS = new PcbChecks.Settings(0.005, 6, 3, CURVE_STEPS,
            new PlacedDesignator.Rules(0.6, 0.9, 1.6, 0.25, 0.6), new Teardrops.Shape(8, 0.9, 0.8, 14, 24));

    private static final String OUTLINE = "M-400 -400H400V400H-400Z";

    private static final String LINK = SETTINGS.autorouting().link().footprint();

    private static final double[][] RING = new double[16][];

    private static final double[] DISTANCES = {0.5, 1.5, 3, 6, 12};

    static {
        for (int step = 0; step < 16; step++) {
            double angle = 2 * Math.PI * step / 16;
            RING[step] = new double[] {Math.cos(angle), Math.sin(angle)};
        }
    }

    private static <T> T read(String resource, Class<T> type) {
        try (InputStream stream = RoutingSettings.class.getResourceAsStream(resource)) {
            return JsonMapper.builder().build().readValue(stream, type);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    // ─── a board ─────────────────────────────────────────────────────────────────────────────────────────

    private static FootprintDrawing.Pad pad(String number, double x, double y) {
        return new FootprintDrawing.Pad(number, "surface", "rectangle", x, y, 0, 1, 1.2, 0, 0, 0, "", "", "", null, null);
    }

    /** A part of two pads either side of its middle. */
    private static final FootprintDrawing CHIP = new FootprintDrawing(List.of(pad("1", -1.5, 0), pad("2", 1.5, 0)), List.of(), List.of(), null, List.of());

    /** A through-hole pin, as a transformer's are: a round pad 2.3 mm across. */
    private static final FootprintDrawing PIN = new FootprintDrawing(List.of(new FootprintDrawing.Pad("1", "through-hole", "circle", 0, 0, 0, 2.3, 2.3,
            1, 1, 0, "", "", "", null, null)), List.of(), List.of(), null, List.of());

    private static Point point(double x, double y) {
        return new Point(x, y);
    }

    private static List<Point> line(double... coordinates) {
        List<Point> points = new ArrayList<>();
        for (int index = 0; index < coordinates.length; index += 2) {
            points.add(point(coordinates[index], coordinates[index + 1]));
        }
        return points;
    }

    private static Track track(List<Point> points) {
        return track(points, PcbVocabulary.TOP, 0.25);
    }

    private static Track track(List<Point> points, String layer, double width) {
        return new Track(layer, width, 0, points);
    }

    private static Placement place(String designator, double x, double y) {
        return new Placement(designator, x, y, 0, PcbVocabulary.TOP, Optional.empty());
    }

    private static Via via(double x, double y, double diameter) {
        return new Via(x, y, 0.3, diameter, PcbVocabulary.TOP, PcbVocabulary.BOTTOM);
    }

    private static PcbDrawing pcb(List<Placement> placements, List<Track> tracks, List<Via> vias,
                                  List<Zone> zones, List<NetClass> netClasses) {
        return new PcbDrawing("circuit", Optional.empty(), netClasses, null, List.of(), List.of(), 2, OUTLINE, placements, tracks, List.of(), vias,
                zones, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static PcbDrawing pcb(List<Placement> placements, List<Track> tracks) {
        return pcb(placements, tracks, List.of(), List.of(), List.of());
    }

    /** Each part's pads on nets: designator, then pad number and net, in pairs. */
    private static Map<String, Map<String, String>> nets(String... wiring) {
        Map<String, Map<String, String>> parts = new LinkedHashMap<>();
        for (int index = 0; index < wiring.length; index += 3) {
            parts.computeIfAbsent(wiring[index], ignored -> new LinkedHashMap<>()).put(wiring[index + 1], wiring[index + 2]);
        }
        return parts;
    }

    private static RoutingBoard board(PcbDrawing pcb, Map<String, Map<String, String>> parts, FootprintDrawing footprint) {
        Map<String, FootprintDrawing> footprints = new LinkedHashMap<>();
        Map<String, Optional<String>> named = new LinkedHashMap<>();
        pcb.placements().forEach(placement -> {
            footprints.put(placement.designator(), footprint);
            named.put(placement.designator(), Optional.of("TEST:PART:F"));
        });
        return new RoutingBoard(footprints, named, parts, VALUES, Map.of(LINK, CHIP), CHECKS, new PadOutline(CURVE_STEPS, 6), SETTINGS);
    }

    private static RoutingBoard board(PcbDrawing pcb, Map<String, Map<String, String>> parts) {
        return board(pcb, parts, CHIP);
    }

    private static TrackRouter.LayingStyle style(double angleStep, Tightening.JogHandling jogs) {
        return new TrackRouter.LayingStyle(angleStep, jogs);
    }

    private static final TrackRouter.LayingStyle WALKING = style(45, Tightening.JogHandling.KEPT);

    // ─── what is wrong with a line ───────────────────────────────────────────────────────────────────────

    private static double across(Point from, Point to) {
        double dx = Math.abs(to.x() - from.x());
        double dy = Math.abs(to.y() - from.y());
        return Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy);
    }

    private static boolean stretchesCross(Point first, Point second, Point third, Point fourth) {
        double one = side(first, second, third);
        double two = side(first, second, fourth);
        double three = side(third, fourth, first);
        double four = side(third, fourth, second);
        return one * two < -1e-9 && three * four < -1e-9;
    }

    private static double side(Point a, Point b, Point c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    /** What is wrong with a line, in words: none is the rule. */
    private static List<String> faultsOfLine(List<Point> points, boolean octilinear) {
        List<String> faults = new ArrayList<>();
        String said = points.toString();
        for (int at = 1; at < points.size(); at++) {
            double dx = points.get(at).x() - points.get(at - 1).x();
            double dy = points.get(at).y() - points.get(at - 1).y();
            double length = Math.hypot(dx, dy);
            if (length < 1e-3) {
                faults.add("two points in one place: " + said);
            }
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            if (octilinear && Math.abs(angle / 45 - Math.round(angle / 45)) > 0.02) {
                faults.add("a stretch at " + angle + "°: " + said);
            }
            if (at >= 2) {
                Point before = points.get(at - 1);
                Point was = points.get(at - 2);
                double inward = ((before.x() - was.x()) * dx + (before.y() - was.y()) * dy) / (was.distanceTo(before) * length);
                if (inward < -1e-3) {
                    faults.add("a stretch turning back on the one before it: " + said);
                }
            }
        }
        for (int first = 1; first < points.size(); first++) {
            for (int second = first + 2; second < points.size(); second++) {
                if (stretchesCross(points.get(first - 1), points.get(first), points.get(second - 1), points.get(second))) {
                    faults.add("it crosses itself: " + said);
                }
            }
        }
        return faults;
    }

    private static List<String> faultsOfLine(List<Point> points) {
        return faultsOfLine(points, true);
    }

    /** What is wrong with a track where it stands on a pcb: its shape, and what it comes too near. */
    private static List<String> faultsOfTrack(RoutingBoard board, PcbDrawing pcb, int index, boolean octilinear) {
        Track line = pcb.tracks().get(index);
        RoutingWorld world = new RoutingWorld(pcb, board);
        TrackFence fence = world.fence(line.layer(), world.netOfTrack(index), line.width(), OptionalInt.of(index));
        List<String> faults = new ArrayList<>(faultsOfLine(line.points(), octilinear));
        if (!fence.lineClear(line.points())) {
            faults.add("it comes too near other copper: " + line.points());
        }
        return faults;
    }

    private static List<String> faultsOfTrack(RoutingBoard board, PcbDrawing pcb, int index) {
        return faultsOfTrack(board, pcb, index, true);
    }

    private static void assertClean(List<String> faults, String about) {
        assertEquals("", String.join(" | ", faults), about);
    }

    private static void assertLine(List<Point> expected, List<Point> actual) {
        assertEquals(expected.size(), actual.size(), "" + actual);
        for (int index = 0; index < expected.size(); index++) {
            assertTrue(expected.get(index).distanceTo(actual.get(index)) < 1e-6, "expected " + expected + " but was " + actual);
        }
    }

    /** How far the farthest corner of one line stands from another. */
    private static double farFrom(List<Point> line, List<Point> other) {
        double farthest = 0;
        for (Point point : line) {
            double nearest = Double.POSITIVE_INFINITY;
            for (int index = 1; index < other.size(); index++) {
                nearest = Math.min(nearest, toStretch(point, other.get(index - 1), other.get(index)));
            }
            farthest = Math.max(farthest, nearest);
        }
        return farthest;
    }

    private static double toStretch(Point point, Point from, Point to) {
        double ax = to.x() - from.x();
        double ay = to.y() - from.y();
        double square = ax * ax + ay * ay;
        double share = square == 0 ? 0 : Math.max(0, Math.min(1, ((point.x() - from.x()) * ax + (point.y() - from.y()) * ay) / square));
        return Math.hypot(point.x() - from.x() - share * ax, point.y() - from.y() - share * ay);
    }

    /** A little generator, so a run of pulls is the same each time. */
    private static final class Generator {
        private long state;

        Generator(long seed) {
            state = seed;
        }

        double next() {
            state = (state * 1664525 + 1013904223) % 4294967296L;
            return state / 4294967296.0;
        }
    }

    private static Autorouter.Routed autorouted(RoutingBoard board, PcbDrawing pcb, double angleStep, double rounding, Autorouter.Crossing crossing,
                                                java.util.function.Consumer<Autorouter.Moment> moments) {
        return Autorouter.route(board, new Autorouter.Request(pcb, angleStep, rounding, crossing, Set.of(), () -> false, moments));
    }

    private static Autorouter.Routed autorouted(RoutingBoard board, PcbDrawing pcb) {
        return autorouted(board, pcb, 45, 0, Autorouter.Crossing.VIAS, moment -> {
        });
    }

    // ─── the cases ───────────────────────────────────────────────────────────────────────────────────────

    /** Where the copper's lines end and meet, each told by what meets there. */
    @Nested
    class ItsCopperNodes {

        private CopperNodes nodesOf(PcbDrawing pcb) {
            return new RoutingWorld(pcb, board(pcb, nets("U1", "1", "A", "U1", "2", "B"))).nodes();
        }

        @Test
        void aTrackFromAPadEndsOnThePadAndItsOtherEndIsAFreeTip() {
            PcbDrawing pcb = pcb(List.of(place("U1", 0, 0)), List.of(track(line(-1.5, 0, -1.5, 6))));
            CopperNodes nodes = nodesOf(pcb);
            CopperNode pad = nodes.atEnd(0, CopperNode.End.FIRST);
            assertEquals(CopperNodeKind.PAD, pad.kind());
            assertLine(line(-1.5, 0), List.of(pad.at()));
            CopperNode tip = nodes.atEnd(0, CopperNode.End.LAST);
            assertEquals(CopperNodeKind.TRACK_TIP, tip.kind());
            assertEquals(OptionalInt.of(0), tip.item());
            // The pad that nothing meets is a node too.
            assertEquals(2, nodes.all().stream().filter(node -> node.kind() == CopperNodeKind.PAD).count());
        }

        @Test
        void twoTracksEndToEndAFewHundredthsApartAreOneJointAndAThirdMakesItAJunction() {
            PcbDrawing joined = pcb(List.of(), List.of(track(line(0, 0, 5, 0)), track(line(5.02, 0, 10, 0))));
            CopperNode joint = nodesOf(joined).atEnd(0, CopperNode.End.LAST);
            assertEquals(CopperNodeKind.JOINT, joint.kind());
            assertEquals(new CopperNode.TrackEnd(1, CopperNode.End.FIRST), joint.partnerOf(0));
            PcbDrawing branched = pcb(List.of(), List.of(track(line(0, 0, 5, 0)), track(line(5.02, 0, 10, 0)), track(line(5, 0, 5, 5))));
            assertEquals(CopperNodeKind.JUNCTION, nodesOf(branched).atEnd(2, CopperNode.End.FIRST).kind());
        }

        @Test
        void aTrackEndingOnTheMiddleOfAnotherIsAT() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 0)), track(line(5, 0, 5, 5))));
            CopperNode tee = nodesOf(pcb).atEnd(1, CopperNode.End.FIRST);
            assertEquals(CopperNodeKind.JUNCTION, tee.kind());
            assertEquals(List.of(0), tee.through());
        }

        @Test
        void aTrackEndingInAViaIsTheViasOnEveryLayerItJoins() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 5, 0)), track(line(5.1, 0, 5.1, 5), PcbVocabulary.BOTTOM, 0.25)),
                    List.of(via(5, 0, 0.6)), List.of(), List.of());
            CopperNodes nodes = nodesOf(pcb);
            CopperNode via = nodes.atEnd(0, CopperNode.End.LAST);
            assertEquals(CopperNodeKind.VIA, via.kind());
            assertSame(via, nodes.atEnd(1, CopperNode.End.FIRST));
            assertTrue(via.on(PcbVocabulary.TOP) && via.on(PcbVocabulary.BOTTOM));
        }

        @Test
        void theNodeNearestAPointIsFoundWithinAReachAndNotPastIt() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 5, 0))));
            CopperNodes nodes = nodesOf(pcb);
            assertEquals(CopperNodeKind.TRACK_TIP, nodes.near(PcbVocabulary.TOP, point(6, 0), 1.5, node -> true).orElseThrow().kind());
            assertTrue(nodes.near(PcbVocabulary.TOP, point(8, 0), 1.5, node -> true).isEmpty());
            assertTrue(nodes.near(PcbVocabulary.BOTTOM, point(6, 0), 1.5, node -> true).isEmpty());
        }
    }

    @Nested
    class APartCarriedItsTrackFollowing {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");

        private Point padOf(Placement placement, int side) {
            return point(placement.x() + side * 1.5, placement.y());
        }

        /** The part B moved by an offset, as the editor says it: the part alone — the copper it carries is the engine's to work out. */
        private PcbDrawing moved(PcbDrawing pcb, double dx, double dy) {
            return pcb.withPlacements(pcb.placements().stream().map(placement -> placement.designator().equals("B")
                    ? new Placement("B", placement.x() + dx, placement.y() + dy, 0, PcbVocabulary.TOP, Optional.empty()) : placement).toList());
        }

        private boolean onThePartBeside(double dx, double dy) {
            return Math.abs(12 + dx) < 5 && Math.abs(dy) < 3;
        }

        /**
         * A frame and a release are one working out: the track pulled tight from the pad that stays to the moved one, its run where it
         * ran and stepping in at 45° — and let go, the layout is what the frame showed.
         */
        @Test
        void aFrameShowsWhatALetGoKeeps() {
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            PcbDrawing pcb = pcb(List.of(a, b), List.of(track(List.of(padOf(a, 1), padOf(b, -1)))));
            RoutingEngine engine = new RoutingEngine(board(pcb, parts));
            PcbDrawing following = engine.tracksBanded(drag(pcb, moved(pcb, 3, 5), 45));
            assertLine(line(1.5, 0, 8.5, 0, 13.5, 5), following.tracks().getFirst().points());
            RoutingEngine.Release.Laid laid = assertInstanceOf(RoutingEngine.Release.Laid.class,
                    engine.tracksLaid(drag(pcb, moved(pcb, 3, 5), 45)));
            assertEquals(following, laid.pcb());
        }

        /**
         * Reconnecting, a frame of the drag is the rigid frame with each line the part pulls stretched straight to its pad where the
         * pad now stands — never left at the pad's old place —; the rubber band round what it meets is the release's.
         */
        @Test
        void aReconnectingFrameStretchesTheLineStraightToThePad() {
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            PcbDrawing pcb = pcb(List.of(a, b), List.of(track(List.of(padOf(a, 1), padOf(b, -1)))));
            RoutingEngine engine = new RoutingEngine(board(pcb, parts));
            List<Point> ran = pcb.tracks().getFirst().points();
            List<Point> frame = engine.tracksFollowing(drag(pcb, moved(pcb, 3, 5), 45)).tracks().getFirst().points();
            assertEquals(ran.getFirst(), frame.getFirst());
            assertEquals(new Point(ran.getLast().x() + 3, ran.getLast().y() + 5), frame.getLast());
        }

        /** Detached, a part moves alone: its line stays where it ran, and only the ratsnest says the pad is to be reached again. */
        @Test
        void aPartMovedDetachedLeavesItsLineWhereItRan() {
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            PcbDrawing pcb = pcb(List.of(a, b), List.of(track(List.of(padOf(a, 1), padOf(b, -1)))));
            RoutingEngine engine = new RoutingEngine(board(pcb, parts));
            RoutingEngine.Release.Laid laid = assertInstanceOf(RoutingEngine.Release.Laid.class,
                    engine.tracksLaid(new PartDrag(pcb, moved(pcb, 3, 5), 45, PartDragMode.DETACH)));
            assertEquals(pcb.tracks(), laid.pcb().tracks());
            assertEquals(15, laid.pcb().placements().get(1).x());
        }

        /**
         * A line the move pulls that cannot be laid — a wall of another net's copper with no way through between its pads — is cut back
         * to what holds it, the move kept: never the whole move refused for one line, as it was until 2026-11-15.
         */
        @Test
        void aLineThatCannotBeLaidAgainIsCutBackAndTheMoveKept() {
            Map<String, Map<String, String>> wired = nets("A", "2", "N", "B", "1", "N", "C", "1", "M");
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            Placement c = place("C", -60, 5);
            PcbDrawing pcb = pcb(List.of(a, b, c), List.of(track(List.of(padOf(a, 1), padOf(b, -1))), track(List.of(padOf(c, -1), point(100, 5)))));
            RoutingEngine engine = new RoutingEngine(board(pcb, wired));
            // B carried across the wall: N's line would have to cross M's.
            RoutingEngine.Release.Laid laid = assertInstanceOf(RoutingEngine.Release.Laid.class, engine.tracksLaid(drag(pcb, moved(pcb, 0, 10), 45)));
            assertEquals(1, laid.pcb().tracks().size(), "N's line cut back to A's pad, M's wall as it ran");
            assertEquals(pcb.tracks().get(1), laid.pcb().tracks().getFirst());
            assertTrue(new RoutingWorld(laid.pcb(), board(laid.pcb(), wired)).meetingsNotIn(new RoutingWorld(pcb, board(pcb, wired))).isEmpty());
        }

        /**
         * A line cut back is laid afresh from its pad to the node it was cut back to where the free room has a way — round the wall's
         * far end, which the band, held to the way round it had, never takes —: the move kept, the pads joined again, every gap kept.
         */
        @Test
        void aLineCutBackIsLaidAfreshRoundTheWayTheBandCouldNotGo() {
            Map<String, Map<String, String>> wired = nets("A", "2", "N", "B", "1", "N", "C", "1", "M", "C", "2", "M");
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            Placement c = place("C", -20, 5);
            PcbDrawing pcb = pcb(List.of(a, b, c), List.of(track(List.of(padOf(a, 1), padOf(b, -1))), track(List.of(padOf(c, -1), point(18, 5)))));
            RoutingBoard board = board(pcb, wired);
            // B carried across the wall: the band cannot cross it; a way round its far end, past x = 18, is open.
            RoutingEngine.Release.Laid laid = assertInstanceOf(RoutingEngine.Release.Laid.class,
                    new RoutingEngine(board).tracksLaid(drag(pcb, moved(pcb, 0, 10), 45)));
            List<Track> tracks = laid.pcb().tracks();
            assertEquals(2, tracks.size(), "M's wall, and N's line laid afresh: " + tracks);
            assertTrue(new RoutingWorld(laid.pcb(), board(laid.pcb(), wired)).nets().unlaid().isEmpty(), "A and B joined again");
            for (int index = 0; index < tracks.size(); index++) {
                assertClean(faultsOfTrack(board(laid.pcb(), wired), laid.pcb(), index), "track " + index + " of " + tracks);
            }
        }

        /**
         * A part let go where its pad would stand on a via of another net is not taken: the via cannot give way, and copper of two
         * nets never meets because of a move. Said where they would meet.
         */
        @Test
        void aPartLetGoWhereItsPadWouldStandOnCopperOfAnotherNetIsRefused() {
            Map<String, Map<String, String>> wired = nets("A", "2", "N", "B", "1", "N", "C", "1", "M");
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            Placement c = place("C", 0, 10);
            Point stitch = point(20, 4);
            PcbDrawing pcb = pcb(List.of(a, b, c), List.of(track(List.of(padOf(a, 1), padOf(b, -1))), track(List.of(padOf(c, -1), stitch))),
                    List.of(via(stitch.x(), stitch.y(), 0.8)), List.of(), List.of());
            RoutingEngine engine = new RoutingEngine(board(pcb, wired));
            // B's pad 1 — at its left, 1.5 off its middle — carried onto the via.
            PcbDrawing moved = moved(pcb, stitch.x() + 1.5 - b.x(), stitch.y() - b.y());
            RoutingEngine.Release release = engine.tracksLaid(drag(pcb, moved, 45));
            RoutingEngine.Release.Refused refused = assertInstanceOf(RoutingEngine.Release.Refused.class, release);
            // Said as the copper that would join them: M's track ends where the via is, now under B's pad.
            assertTrue(refused.reason().contains("C pad 1 of M would reach B pad 1 of N through Track 2"), refused.reason());
        }

        @Test
        void keepsAShapelyLineWhereverItGoesInFreeSpace() {
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            Map<String, java.util.function.BiFunction<Point, Point, List<Point>>> shapes = new LinkedHashMap<>();
            shapes.put("straight", (from, to) -> List.of(from, to));
            shapes.put("a jog", (from, to) -> List.of(from, point(from.x() + 2, from.y()), point(to.x() - 2, to.y()), to));
            shapes.put("a detour", (from, to) -> List.of(from, point(from.x() + 2, from.y()), point(from.x() + 2, from.y() + 4),
                    point(to.x() - 2, from.y() + 4), point(to.x() - 2, to.y()), to));
            shapes.forEach((name, shape) -> {
                PcbDrawing pcb = pcb(List.of(a, b), List.of(track(shape.apply(padOf(a, 1), padOf(b, -1)))));
                RoutingBoard board = board(pcb, parts);
                RoutingEngine engine = new RoutingEngine(board);
                for (double[] way : RING) {
                    for (double distance : DISTANCES) {
                        double dx = way[0] * distance;
                        double dy = way[1] * distance;
                        if (onThePartBeside(dx, dy)) {
                            continue;
                        }
                        PcbDrawing laid = laidBy(engine, pcb, moved(pcb, dx, dy), 45);
                        List<Point> line = laid.tracks().getFirst().points();
                        assertClean(faultsOfTrack(board, laid, 0), name + " by " + dx + ", " + dy);
                        if (name.equals("a detour")) {
                            // A way round nothing the reader laid comes no longer for the drag than the part went: a rubber band slips
                            // off a corner it folds at, and where it does not it stands as the reader laid it.
                            List<Point> laidByReader = shape.apply(padOf(a, 1), padOf(b, -1));
                            assertTrue(Polylines.lengthOf(line) <= Polylines.lengthOf(laidByReader) + Math.hypot(dx, dy) + 0.01, name + " by " + dx + ", " + dy + ": " + line);
                        } else {
                            // Pulled as a rubber band, a jog round nothing comes as short as its ends allow.
                            assertTrue(Polylines.lengthOf(line) <= across(line.getFirst(), line.getLast()) + 0.01, name + " by " + dx + ", " + dy + ": " + line);
                        }
                    }
                }
            });
        }

        @Test
        void goesRoundWhatStandsInTheWay() {
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            PcbDrawing pcb = pcb(List.of(a, b), List.of(track(List.of(padOf(a, 1), padOf(b, -1)))), List.of(via(6, 6, 1.2)), List.of(), List.of());
            RoutingBoard board = board(pcb, parts);
            RoutingEngine engine = new RoutingEngine(board);
            for (double[] way : RING) {
                for (double distance : DISTANCES) {
                    double dx = way[0] * distance;
                    double dy = way[1] * distance;
                    if (onThePartBeside(dx, dy)) {
                        continue;
                    }
                    assertClean(faultsOfTrack(board, laidBy(engine, pcb, moved(pcb, dx, dy), 45), 0), "by " + dx + ", " + dy);
                }
            }
        }

        @Test
        void answersTheSameAFrameAtATimeAsWhenItIsLetGoInFreeSpace() {
            Placement a = place("A", 0, 0);
            Placement b = place("B", 12, 0);
            PcbDrawing pcb = pcb(List.of(a, b), List.of(track(List.of(padOf(a, 1), padOf(b, -1)))));
            RoutingBoard board = board(pcb, parts);
            RoutingEngine engine = new RoutingEngine(board);
            for (double[] way : RING) {
                assertClean(faultsOfTrack(board, engine.tracksBanded(drag(pcb, moved(pcb, way[0] * 4, way[1] * 4), 45)), 0), "towards " + way[0] + ", " + way[1]);
            }
        }
    }

    /**
     * The editor says only what it moved; the copper a part carries is the engine's to work out, against the pads as they are —
     * their shape, their layers, their nets —, never their boxes.
     */
    @Nested
    class CopperAPartCarries {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");

        private Placement placed(String designator, double x, double y, double rotation, String side) {
            return new Placement(designator, x, y, rotation, side, Optional.empty());
        }

        private PcbDrawing ended(PcbDrawing pcb, Placement b) {
            return new RoutingEngine(board(pcb, parts)).tracksBanded(drag(pcb, pcb.withPlacements(List.of(pcb.placements().getFirst(), b)),
                    45));
        }

        @Test
        void anEndAnywhereOnThePadGoesToTheSameSpotOfIt() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 0)), List.of(track(line(1.5, 0, 6, 0, 10.5, 0.4))));
            assertEquals(point(10.5, 3.4), ended(pcb, place("B", 12, 3)).tracks().getFirst().points().getLast());
        }

        @Test
        void aTurnedPartCarriesTheEndToWhereTheTurnTookThatSpot() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 0)), List.of(track(line(1.5, 0, 6, 0, 10.5, 0))));
            Placement turned = placed("B", 12, 0, 90, PcbVocabulary.TOP);
            Point pad = turned.placed(-1.5, 0);
            Point end = ended(pcb, turned).tracks().getFirst().points().getLast();
            assertTrue(Precision.samePoint(pad, end), "the end stands at " + end + ", the pad at " + pad);
        }

        @Test
        void aPartPutOnTheOtherSideLeavesTheTracksOfTheSideItLeft() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 0)), List.of(track(line(1.5, 0, 6, 0, 10.5, 0))));
            assertEquals(pcb.tracks(), ended(pcb, placed("B", 12, 0, 0, PcbVocabulary.BOTTOM)).tracks());
        }

        @Test
        void anArcHeldByOneEndGoesWithItsPadKeepingItsBulge() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 0)), List.of())
                    .withArcs(List.of(new Arc(PcbVocabulary.TOP, 0.25, point(1.5, 0), point(6, 3), point(10.5, 0))));
            Arc arc = new RoutingEngine(board(pcb, parts)).tracksBanded(drag(pcb, pcb.withPlacements(List.of(place("A", 0, 0),
                    place("B", 12, 2))), 45)).arcs().getFirst();
            assertEquals(point(1.5, 0), arc.from());
            assertEquals(point(10.5, 2), arc.to());
            assertEquals(point(6, 4), arc.through());
        }

        /** A round pad's box is not the pad: copper of another net beside its round edge, inside the box, stays where it is. */
        @Test
        void copperOfAnotherNetBesideARoundPadIsNotCarried() {
            Map<String, Map<String, String>> wired = nets("A", "1", "N", "B", "1", "N", "C", "1", "M");
            List<Placement> placements = List.of(place("A", 0, 0), place("B", 12, 0), place("C", 30, 1));
            PcbDrawing pcb = pcb(placements, List.of(track(line(0, 0, 12, 0)), track(line(12.95, 0.95, 20, 1, 30, 1))));
            RoutingEngine engine = new RoutingEngine(board(pcb, wired, PIN));
            PcbDrawing moved = pcb.withPlacements(List.of(placements.get(0), place("B", 12, 4), placements.get(2)));
            PcbDrawing following = engine.tracksBanded(drag(pcb, moved, 45));
            assertEquals(point(12, 4), following.tracks().getFirst().points().getLast());
            assertEquals(pcb.tracks().get(1), following.tracks().get(1));
        }
    }

    /**
     * What a moved part comes to stand on: another net's track in its way is pushed ahead of it — a band bent round the part where
     * it is pushed, the rest of it where it ran —; copper the part does not come up against is not touched. A frame shows what the
     * release keeps.
     */
    @Nested
    class WhatAMovedPartStandsOn {

        // A and B on N, joined; C and D on M, joined by a long track the length of the board; B is put down on it.
        private final Map<String, Map<String, String>> wired = nets("A", "2", "N", "B", "1", "N", "C", "2", "M", "D", "1", "M");
        private final List<Placement> placements = List.of(place("A", 0, 0), place("B", 12, 0), place("C", -20, 6), place("D", 40, 6));
        // Net-less copper too near A's pad 1, standing so before the drag, nowhere near where B goes.
        private final Track tooNear = track(line(-6, -0.9, -1.5, -0.9));
        private final PcbDrawing pcb = pcb(placements, List.of(track(line(1.5, 0, 10.5, 0)), track(line(-18.5, 6, 38.5, 6)), tooNear));
        private final PcbDrawing moved = pcb.withPlacements(List.of(placements.get(0), place("B", 12, 6), placements.get(2), placements.get(3)));
        private final RoutingBoard board = board(pcb, wired);

        /** Pushed ahead where the part came up against it: both ends and the far runs where they were, nothing too near; B's own track laid too. */
        private void assertPushedAhead(PcbDrawing laid) {
            List<Point> pushed = laid.tracks().get(1).points();
            assertEquals(point(-18.5, 6), pushed.getFirst());
            assertEquals(point(38.5, 6), pushed.getLast());
            assertTrue(pushed.get(1).x() > 0 && pushed.get(pushed.size() - 2).x() < 25, "the far runs stay where they ran: " + pushed);
            assertTrue(pushed.stream().allMatch(point -> point.y() >= 6 - 1e-9), "pushed ahead of the part, never jumped over: " + pushed);
            assertClean(faultsOfTrack(board, laid, 1), "the track B was put down on");
            assertClean(faultsOfTrack(board, laid, 0), "B's own track");
            assertEquals(tooNear, laid.tracks().get(2));
        }

        @Test
        void theTrackItIsPutDownOnIsPushedAheadOfIt() {
            RoutingEngine engine = new RoutingEngine(board);
            PcbDrawing frame = engine.tracksBanded(drag(pcb, moved, 45));
            assertPushedAhead(frame);
            assertEquals(frame, assertInstanceOf(RoutingEngine.Release.Laid.class, engine.tracksLaid(drag(pcb, moved, 45))).pcb());
        }

        /**
         * A part put down on a track whose other half — the same net, met at a junction on the part's own pad — the layout now
         * joins to other nets through the pads standing on it: that half is still its net's, never another's copper to keep
         * clear of, so the half pressed on is laid round ahead of the part into the junction. As Q2, a TO-247 with pads 2.5 by
         * 4.5 apart by 5.45, put down on net_Q1_B, 2026-11-14.
         */
        @Test
        void theOtherHalfOfATrackItsOwnPadMeetsIsNotAnotherNetsCopper() {
            FootprintDrawing.Pad first = new FootprintDrawing.Pad("1", "through-hole", "oval", -5.45, 0, 0, 2.5, 4.5, 1.2, 1.2, 0, "", "", "", null, null);
            FootprintDrawing.Pad second = new FootprintDrawing.Pad("2", "through-hole", "oval", 0, 0, 0, 2.5, 4.5, 1.2, 1.2, 0, "", "", "", null, null);
            FootprintDrawing.Pad third = new FootprintDrawing.Pad("3", "through-hole", "oval", 5.45, 0, 0, 2.5, 4.5, 1.2, 1.2, 0, "", "", "", null, null);
            FootprintDrawing transistor = new FootprintDrawing(List.of(first, second, third), List.of(), List.of(), null, List.of());
            Map<String, Map<String, String>> wired = nets("B", "1", "M", "B", "2", "N", "B", "3", "E", "C", "3", "M", "D", "1", "M");
            List<Placement> standing = List.of(place("B", 12, 11), place("C", -25, 6), place("D", 45.45, 6));
            Track firstHalf = track(line(-19.55, 6, 6.8, 6), PcbVocabulary.TOP, 1.5);
            PcbDrawing junctioned = pcb(standing, List.of(firstHalf, track(line(6.8, 6, 40, 6), PcbVocabulary.TOP, 1.5),
                    track(line(6.55, 11, 6.8, 6), PcbVocabulary.TOP, 1.5)));
            PcbDrawing putDown = junctioned.withPlacements(List.of(place("B", 12, 6), standing.get(1), standing.get(2)));
            RoutingBoard junctionBoard = board(junctioned, wired, transistor);
            PcbDrawing laid = assertInstanceOf(RoutingEngine.Release.Laid.class, new RoutingEngine(junctionBoard)
                    .tracksLaid(drag(junctioned, putDown, 45))).pcb();
            assertEquals(firstHalf, laid.tracks().getFirst());
            assertClean(faultsOfTrack(junctionBoard, laid, 1), "the half B was put down on");
        }
    }

    /**
     * A drag's frames are each worked out from where the drag began, never from the frame before: a track is shown whole as the part
     * pulls it, never broken off nor taken away (U1, 2026-11-14), and a hand that wandered leaves nothing behind (BZ1).
     */
    @Nested
    class FramesOfADrag {

        /**
         * A part wandered with back and forth, round and round, then let go, leaves the copper as one straight drag to the same place
         * leaves it: no frame is worked out from another, so no knot gathers (Ivan's BZ1, 2026-11-14).
         */
        @Test
        void aPartWanderedWithLeavesNothingBehindIt() {
            Map<String, Map<String, String>> wired = nets("A", "2", "N", "B", "1", "N", "C", "1", "M");
            Placement a = place("A", 0, 0);
            Placement c = place("C", 0, 10);
            PcbDrawing pcb = pcb(List.of(a, place("B", 12, 0), c), List.of(track(line(1.5, 0, 10.5, 0)), track(line(-1.5, 10, 6, 3))),
                    List.of(via(6, 3, 0.8)), List.of(), List.of());
            java.util.function.DoubleFunction<PcbDrawing> at = turn -> pcb.withPlacements(List.of(a,
                    place("B", Precision.settled(12 + 4 * Math.cos(turn)), Precision.settled(4 * Math.sin(turn))), c));
            RoutingEngine wandered = new RoutingEngine(board(pcb, wired));
            for (double turn = 0; turn < 6 * Math.PI; turn += 0.3) {
                wandered.tracksBanded(new PartDrag(pcb, at.apply(turn), 45, PartDragMode.RECONNECT));
            }
            PcbDrawing last = at.apply(6 * Math.PI);
            RoutingEngine.Release afterWandering = wandered.tracksLaid(new PartDrag(pcb, last, 45, PartDragMode.RECONNECT));
            RoutingEngine.Release straightThere = new RoutingEngine(board(pcb, wired))
                    .tracksLaid(new PartDrag(pcb, last, 45, PartDragMode.RECONNECT));
            assertEquals(straightThere, afterWandering);
        }

        @Test
        void neverBreakATrackOff() {
            Map<String, Map<String, String>> wired = nets("A", "2", "N", "B", "1", "N", "C", "1", "M");
            Placement a = place("A", 0, 0);
            Placement c = place("C", 0, 10);
            // M's via stands between A and where B goes: a straight step from A's pad to B's is never clear.
            PcbDrawing pcb = pcb(List.of(a, place("B", 12, 0), c), List.of(track(line(1.5, 0, 10.5, 0)), track(line(-1.5, 10, 6, 3))),
                    List.of(via(6, 3, 0.8)), List.of(), List.of());
            RoutingEngine engine = new RoutingEngine(board(pcb, wired));
            for (double up = 0.5; up <= 6; up += 0.5) {
                PcbDrawing moved = pcb.withPlacements(List.of(a, place("B", 12, up), c));
                PcbDrawing shown = engine.tracksBanded(new PartDrag(pcb, moved, 45, PartDragMode.RECONNECT));
                assertEquals(pcb.tracks().size(), shown.tracks().size(), "a track was taken off at " + up);
                List<Point> line = shown.tracks().getFirst().points();
                assertEquals(point(1.5, 0), line.getFirst(), "broken off A's pad at " + up + ": " + line);
                assertTrue(Precision.samePoint(point(10.5, up), line.getLast()), "broken off B's pad at " + up + ": " + line);
            }
        }
    }

    /**
     * A buzzer of two round pins 7.6 mm apart, each with its own net's track, carried every way about where it stood: its pin 1's
     * track never runs past pin 2 and comes back — a hairpin round the other net's pin, where a step straight to the rest of the
     * line was clear (Ivan's BZ1, image 114, 2026-11-14) —, nor crosses itself, nor comes too near other copper; in a frame or let go.
     */
    @Nested
    class ABuzzerCarriedAbout {

        private static final double FARTHEST = 3;

        private static final double STEP = 0.5;

        private final FootprintDrawing buzzer = new FootprintDrawing(List.of(
                new FootprintDrawing.Pad("1", "through-hole", "circle", 0, 0, 0, 2, 2, 1, 1, 0, "", "", "", null, null),
                new FootprintDrawing.Pad("2", "through-hole", "circle", 0, -7.6, 0, 2, 2, 1, 1, 0, "", "", "", null, null)),
                List.of(), List.of(), null, List.of());

        private final Map<String, Map<String, String>> wired = nets("BZ", "1", "PLUS", "BZ", "2", "BUZZER", "C", "1", "PLUS", "D", "1", "BUZZER");

        private final Placement c = place("C", 73.95, 27.85);

        private final Placement d = place("D", 67.175, 43.1);

        // As the control board has them: pin 1's track out to the right and up, pin 2's coming down from above and in to it.
        private final PcbDrawing pcb = pcb(List.of(place("BZ", 70.2, 66.65), c, d), List.of(
                track(line(70.2, 66.65, 73.95, 62.9, 73.95, 27.85), PcbVocabulary.TOP, 0.4),
                track(line(67.175, 43.1, 67.175, 56.025, 70.2, 59.05), PcbVocabulary.TOP, 0.2)));

        private RoutingBoard board() {
            Map<String, FootprintDrawing> footprints = new LinkedHashMap<>();
            Map<String, Optional<String>> named = new LinkedHashMap<>();
            footprints.put("BZ", buzzer);
            footprints.put("C", PIN);
            footprints.put("D", PIN);
            pcb.placements().forEach(placement -> named.put(placement.designator(), Optional.of("TEST:PART:F")));
            return new RoutingBoard(footprints, named, wired, VALUES, Map.of(LINK, CHIP), CHECKS, new PadOutline(CURVE_STEPS, 6), SETTINGS);
        }

        private PartDrag carried(double dx, double dy) {
            PcbDrawing moved = pcb.withPlacements(List.of(place("BZ", Precision.settled(70.2 + dx), Precision.settled(66.65 + dy)), c, d));
            return new PartDrag(pcb, moved, 45, PartDragMode.RECONNECT);
        }

        @Test
        void pinOnesTrackNeverRunsPastPinTwoAndBack() {
            RoutingBoard board = board();
            List<String> faults = new ArrayList<>();
            // One hand wanders over every place, frame after frame, as image 114's drag went; each place is let go from afresh.
            RoutingEngine hand = new RoutingEngine(board);
            for (double dx = -FARTHEST; dx <= FARTHEST; dx += STEP) {
                for (double dy = -FARTHEST; dy <= FARTHEST; dy += STEP) {
                    PartDrag drag = carried(dx, dy);
                    String where = "carried by " + Precision.settled(dx) + ", " + Precision.settled(dy);
                    PcbDrawing frame = hand.tracksBanded(drag);
                    faultsOfTrack(board, frame, 0).forEach(fault -> faults.add(where + ", in a frame: " + fault));
                    switch (new RoutingEngine(board).tracksLaid(drag)) {
                        case RoutingEngine.Release.Laid laid -> faultsOfTrack(board, laid.pcb(), 0).forEach(fault -> faults.add(where + ", let go: " + fault));
                        case RoutingEngine.Release.Refused refused -> faults.add(where + ", refused: " + refused.reason());
                    }
                }
            }
            assertClean(faults, "pin 1's track");
        }

        /**
         * A stub of pin 2's net standing wholly on pin 2 goes with the part step by step, as the pin does — never standing where the
         * part is let go from the first step on, a wall in the way of the line the pin pushes ahead (BZ1, cycle 4, 2026-11-14).
         */
        @Test
        void copperGoingWholeWithThePartMovesWithItStepByStep() {
            PcbDrawing stubbed = pcb(List.of(place("BZ", 70.2, 66.65), c, d), List.of(
                    track(line(70.2, 66.65, 71.8, 65.05, 71.8, 27.85), PcbVocabulary.TOP, 0.4),
                    track(line(67.175, 43.1, 67.175, 56.025, 70.2, 59.05), PcbVocabulary.TOP, 0.2),
                    track(line(70.2, 59.05, 70.9, 59.05), PcbVocabulary.TOP, 0.2)));
            RoutingBoard board = board();
            PcbDrawing laid = laidBy(new RoutingEngine(board), stubbed, stubbed.withPlacements(List.of(place("BZ", 72.7, 66.65), c, d)), 45);
            assertEquals(point(73.4, 59.05), laid.tracks().get(2).points().getLast(), "the stub went with the pin");
            assertClean(faultsOfTrack(board, laid, 0, false), "the line the pin pushed");
        }

        /**
         * Moved and let go, moved and let go, ten times over, every way: each release leaves both tracks clear and as short as their
         * ends allow — never the snakes the moves before would leave were a release to mend only what it changed (image 116).
         */
        @Test
        void movedAndLetGoTimeAfterTimeItsTracksStayStraight() {
            RoutingBoard board = board();
            Generator generator = new Generator(116);
            PcbDrawing standing = pcb;
            double x = 70.2;
            double y = 66.65;
            for (int release = 1; release <= 10; release++) {
                RoutingEngine hand = new RoutingEngine(board);
                Placement put = null;
                for (int move = 0; move < 2; move++) {
                    x = Precision.settled(x + Math.round((generator.next() * 2 - 1) * 6) / 2.0);
                    y = Precision.settled(y + Math.round((generator.next() * 2 - 1) * 6) / 2.0);
                    put = place("BZ", x, y);
                    hand.tracksBanded(new PartDrag(standing, standing.withPlacements(List.of(put, c, d)), 45, PartDragMode.RECONNECT));
                }
                PcbDrawing letGo = standing.withPlacements(List.of(put, c, d));
                standing = switch (hand.tracksLaid(new PartDrag(standing, letGo, 45, PartDragMode.RECONNECT))) {
                    case RoutingEngine.Release.Laid laid -> laid.pcb();
                    case RoutingEngine.Release.Refused refused -> throw new AssertionError("release " + release + " refused: " + refused.reason());
                };
                for (int index = 0; index < 2; index++) {
                    List<Point> line = standing.tracks().get(index).points();
                    String about = "release " + release + " at " + x + ", " + y + ", track " + index;
                    assertClean(faultsOfTrack(board, standing, index), about);
                    // A drag pulls tight only the region it works in, so a line may keep a corner beyond it — never a snake: no more
                    // corners than it was laid with and two, none turning back (faultsOfTrack).
                    int corners = Polylines.cornersOf(line).size() - 2;
                    assertTrue(corners <= Polylines.cornersOf(pcb.tracks().get(index).points()).size(), about + " has " + corners + " corners: " + line);
                }
            }
        }
    }

    /**
     * The joints a drag's lines meet at that are not pads — a third track meeting a line, a via —: held where they stand, as the
     * settings have it by default; or, where the settings let them go, slid to where the lines meeting at them are shortest together.
     */
    @Nested
    class JointsThatSlide {

        private final Map<String, Map<String, String>> wired = nets("P", "1", "N", "Q", "1", "N", "R", "1", "N");

        /** The settings with no joint holding a band: every junction and via slides. */
        private RoutingBoard boardLettingJointsGo(PcbDrawing pcb) {
            RoutingSettings.Banding banding = SETTINGS.banding();
            RoutingSettings sliding = new RoutingSettings(SETTINGS.routing(), SETTINGS.carrying(), new RoutingSettings.Banding(banding.reach(), banding.stepShare(),
                    banding.mostSteps(), banding.margin(), List.of(), banding.nodeRounds(), banding.pushPasses(), banding.jointHalvings()), SETTINGS.pads(), SETTINGS.autorouting(), SETTINGS.tuning());
            return new RoutingBoard(footprintsOf(pcb), namedOf(pcb), wired, VALUES, Map.of(LINK, CHIP), CHECKS, new PadOutline(CURVE_STEPS, 6), sliding);
        }

        private Map<String, FootprintDrawing> footprintsOf(PcbDrawing pcb) {
            Map<String, FootprintDrawing> footprints = new LinkedHashMap<>();
            pcb.placements().forEach(placement -> footprints.put(placement.designator(), PIN));
            return footprints;
        }

        private Map<String, Optional<String>> namedOf(PcbDrawing pcb) {
            Map<String, Optional<String>> named = new LinkedHashMap<>();
            pcb.placements().forEach(placement -> named.put(placement.designator(), Optional.of("TEST:PART:F")));
            return named;
        }

        // Three pins on one net, their tracks meeting at a junction well off where they would meet shortest — near R, which is moved,
        // within the region a drag works in.
        private final List<Placement> placements = List.of(place("P", 0, 0), place("Q", 20, 0), place("R", 10, 20));
        private final PcbDrawing junctioned = pcb(placements, List.of(track(line(0, 0, 10, 17)), track(line(20, 0, 10, 17)), track(line(10, 20, 10, 17))));
        private final PcbDrawing nudged = junctioned.withPlacements(List.of(placements.get(0), placements.get(1), place("R", 10.5, 20)));

        private static double lengthOfAll(PcbDrawing pcb) {
            return pcb.tracks().stream().mapToDouble(track -> Polylines.lengthOf(track.points())).sum();
        }

        /** The drag laid with every joint held, as the settings have it by default. */
        private PcbDrawing laidHeld() {
            return laidBy(new RoutingEngine(new RoutingBoard(footprintsOf(junctioned), namedOf(junctioned), wired, VALUES, Map.of(LINK, CHIP),
                    CHECKS, new PadOutline(CURVE_STEPS, 6), SETTINGS)), junctioned, nudged, 45);
        }

        @Test
        void aJunctionHeldByDefaultStaysWhereItIs() {
            laidHeld().tracks().forEach(track -> assertEquals(point(10, 17), track.points().getLast(), "the junction stays: " + track.points()));
        }

        @Test
        void aJunctionLetGoSlidesToWhereItsLinesAreShortest() {
            RoutingBoard board = boardLettingJointsGo(junctioned);
            PcbDrawing laid = laidBy(new RoutingEngine(board), junctioned, nudged, 45);
            Point joint = laid.tracks().getFirst().points().getLast();
            laid.tracks().forEach(track -> assertTrue(Precision.samePoint(joint, track.points().getLast()), "the lines still meet: " + laid.tracks()));
            // Where three lines from a triangle's corners are shortest together: well below where they met, towards P and Q.
            assertTrue(joint.y() < 15, "the junction slid towards where its lines are shortest: " + joint);
            PcbDrawing held = laidHeld();
            assertTrue(lengthOfAll(laid) < lengthOfAll(held), "shorter together than with the junction held: " + lengthOfAll(laid) + " against " + lengthOfAll(held) + ": " + held.tracks());
            for (int index = 0; index < laid.tracks().size(); index++) {
                assertClean(faultsOfTrack(board, laid, index, false), "track " + index);
            }
        }

        @Test
        void aViaLetGoSlidesToWhereItsLinesAreShortest() {
            // P's track runs on the top to a via off the way to Q — near Q, which is moved —, Q's on the bottom from it.
            PcbDrawing viaed = pcb(placements, List.of(track(line(0, 0, 17, 3)), track(line(17, 3, 20, 0), PcbVocabulary.BOTTOM, 0.25)),
                    List.of(via(17, 3, 0.8)), List.of(), List.of());
            PcbDrawing moved = viaed.withPlacements(List.of(placements.get(0), place("Q", 20.5, 0), placements.get(2)));
            RoutingBoard board = boardLettingJointsGo(viaed);
            PcbDrawing laid = laidBy(new RoutingEngine(board), viaed, moved, 45);
            Via via = laid.vias().getFirst();
            assertTrue(via.y() < 2.5, "the via slid towards the way between the pins: " + via);
            assertTrue(Precision.samePoint(point(via.x(), via.y()), laid.tracks().getFirst().points().getLast()), "the top line still ends on it");
            assertTrue(Precision.samePoint(point(via.x(), via.y()), laid.tracks().get(1).points().getFirst()), "the bottom line still starts on it");
            assertTrue(lengthOfAll(laid) < lengthOfAll(viaed), "shorter together: " + laid.tracks());
        }
    }

    /**
     * A row of pins, each with its track running away from the part, and the part carried sideways: every track comes in to its
     * pin straight out of the row before it turns — between the pins there is no room to turn —, all laid, all clear (U1, 2026-11-14).
     */
    @Nested
    class ARowOfPins {

        private static final int PINS = 6;

        /** The row's pitch: a fine one, where a track has no room to turn between the pins. */
        private static final double PITCH = 0.65;

        @Test
        void carriedSidewaysEveryTrackComesInStraightAndClear() {
            List<FootprintDrawing.Pad> row = new ArrayList<>();
            Map<String, Map<String, String>> wired = new LinkedHashMap<>();
            Map<String, String> pins = new LinkedHashMap<>();
            List<Track> tracks = new ArrayList<>();
            for (int pin = 0; pin < PINS; pin++) {
                double x = Precision.settled((pin - (PINS - 1) / 2.0) * PITCH);
                row.add(new FootprintDrawing.Pad(String.valueOf(pin + 1), "surface", "rectangle", x, 3, 0, 0.35, 1.5, 0, 0, 0, "", "", "", null, null));
                pins.put(String.valueOf(pin + 1), "N" + pin);
                tracks.add(track(line(x, 3, x, 15)));
            }
            wired.put("U", pins);
            FootprintDrawing part = new FootprintDrawing(row, List.of(), List.of(), null, List.of());
            PcbDrawing pcb = pcb(List.of(place("U", 0, 0)), tracks);
            RoutingBoard rowBoard = board(pcb, wired, part);
            PcbDrawing laid = assertInstanceOf(RoutingEngine.Release.Laid.class, new RoutingEngine(rowBoard)
                    .tracksLaid(new PartDrag(pcb, pcb.withPlacements(List.of(place("U", 2 * PITCH, 0))), 45, PartDragMode.RECONNECT))).pcb();
            for (int index = 0; index < PINS; index++) {
                assertClean(faultsOfTrack(rowBoard, laid, index), "track " + index);
            }
        }
    }

    @Nested
    class NothingJumpsUnderThePointer {

        private static final double STEP = 0.25;
        private static final double FARTHEST = 2.5;

        @Test
        void aCorner() {
            for (List<Point> points : List.of(line(0, 0, 6, 6, 16, 6, 22, 0), line(0, 0, 30, 0, 30, 30), line(0, 0, 10, 0, 20, 10, 30, 10))) {
                PcbDrawing pcb = pcb(List.of(), List.of(track(points)));
                RoutingEngine engine = new RoutingEngine(board(pcb, Map.of()));
                for (int corner = 1; corner + 1 < points.size(); corner++) {
                    for (double[] way : RING) {
                        List<Point> last = points;
                        for (double distance = STEP; distance <= 6; distance += STEP) {
                            Point to = point(points.get(corner).x() + way[0] * distance, points.get(corner).y() + way[1] * distance);
                            List<Point> now = engine.cornerCarried(pcb, 0, corner, to, WALKING).tracks().getFirst().points();
                            assertTrue(farFrom(List.of(to), now) <= 0.5, "the corner is left behind at " + to + ": " + now);
                            assertTrue(Math.max(farFrom(now, last), farFrom(last, now)) < FARTHEST, "corner " + corner + " of " + points + " to " + to
                                    + ": " + last + " became " + now);
                            last = now;
                        }
                    }
                }
            }
        }

        @Test
        void aPartsTrack() {
            Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");
            // Lines already tight: one with a way round what is no longer there is pulled tight once, as it should be, and steady after.
            for (List<Point> points : List.of(line(1.5, 0, 10.5, 0), line(1.5, 0, 4.5, 3, 10.5, 3))) {
                double standing = points.getLast().y();
                PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, standing)), List.of(track(points)));
                RoutingEngine engine = new RoutingEngine(board(pcb, parts));
                for (double[] way : RING) {
                    List<Point> last = points;
                    for (double distance = STEP; distance <= 6; distance += STEP) {
                        double dx = way[0] * distance;
                        double dy = way[1] * distance;
                        if (Math.abs(12 + dx) < 5 && Math.abs(standing + dy) < 3) {
                            continue;
                        }
                        PcbDrawing carried = pcb.withPlacements(List.of(place("A", 0, 0), place("B", 12 + dx, standing + dy)));
                        List<Point> now = engine.tracksBanded(drag(pcb, carried, 45)).tracks().getFirst().points();
                        assertTrue(Math.max(farFrom(now, last), farFrom(last, now)) < FARTHEST, "by " + dx + ", " + dy + ": " + last + " became " + now);
                        last = now;
                    }
                }
            }
        }
    }

    @Nested
    class WhatAnEngineerMeansByPullingAStretch {

        // From the pad down a diagonal, along a flat, up a diagonal to the other pad.
        private final List<Point> line = line(0, 0, 6, 6, 16, 6, 22, 0);

        private PcbDrawing carried(List<Point> points, int segment, double dx, double dy) {
            PcbDrawing pcb = pcb(List.of(), List.of(track(points)));
            return new RoutingEngine(board(pcb, Map.of())).segmentCarried(pcb, 0, segment, dx, dy, WALKING, Optional.empty()).orElseThrow();
        }

        // Ivan, 2026-11-06: it leaves the pad with one corner, as KiCad drags a segment: at 45° to the stretch until it meets it.
        @Test
        void movesTheFirstStretchParallelOutOfThePadLevelThenTheSameDiagonalFurtherOn() {
            assertLine(line(0, 0, 4, 0, 10, 6, 16, 6, 22, 0), carried(line, 0, 4, 0).tracks().getFirst().points());
        }

        @Test
        void pullsALastDiagonalIntoItsPadAsideTheRunAboveGrowsAndALevelStepGoesIntoThePad() {
            assertLine(line(0, -10, 0, 1, 4, 5, 5, 5), carried(line(0, -10, 0, 0, 5, 5), 1, -1, 0).tracks().getFirst().points());
        }

        @Test
        void leavesThePadWithOneCornerHoweverLittleOrMuchItIsPulledAtEitherEnd() {
            double[][] pulls = {{0, 0.05, 0}, {0, 0.5, 0}, {0, 3, 0}, {0, -0.2, 0}, {2, -0.05, 0}, {2, -0.5, 0}, {2, -3, 0}, {2, 0, -0.3}};
            for (double[] pull : pulls) {
                List<Point> points = carried(line, (int) pull[0], pull[1], pull[2]).tracks().getFirst().points();
                assertEquals(5, points.size(), "stretch " + pull[0] + " by " + pull[1] + ", " + pull[2] + ": " + points);
            }
        }

        @Test
        void doesTheSameAtTheOtherEndAndAcrossTheOtherWay() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line)));
            RoutingBoard board = board(pcb, Map.of());
            double[][] pulls = {{2, -4, 0}, {0, -3, 0}, {2, 3, 0}, {0, 0, -3}, {2, 0, -3}};
            for (double[] pull : pulls) {
                PcbDrawing answer = new RoutingEngine(board).segmentCarried(pcb, 0, (int) pull[0], pull[1], pull[2], WALKING, Optional.empty()).orElseThrow();
                assertClean(faultsOfTrack(board, answer, 0), "stretch " + pull[0] + " by " + pull[1] + ", " + pull[2]);
                assertTrue(answer.tracks().getFirst().points().size() <= 6);
            }
        }

        @Test
        void movesTheFlatParallelItsNeighboursKeepingTheirSlopes() {
            assertLine(line(0, 0, 8, 8, 14, 8, 22, 0), carried(line, 1, 0, 2).tracks().getFirst().points());
        }

        @Test
        void neverJumpsALittleMorePullALittleMoreChange() {
            record Shape(List<Point> points, int segment) {
            }
            List<Shape> shapes = List.of(new Shape(line, 0), new Shape(line, 1), new Shape(line, 2), new Shape(line(0, 0, 30, 0), 0),
                    new Shape(line(0, 0, 10, 0, 20, 10, 30, 10), 1));
            for (Shape shape : shapes) {
                PcbDrawing pcb = pcb(List.of(), List.of(track(shape.points())));
                RoutingEngine engine = new RoutingEngine(board(pcb, Map.of()));
                for (double[] way : RING) {
                    List<Point> last = shape.points();
                    for (double distance = 0.25; distance <= 6; distance += 0.25) {
                        Optional<PcbDrawing> answer = engine.segmentCarried(pcb, 0, shape.segment(), way[0] * distance, way[1] * distance, WALKING, Optional.empty());
                        List<Point> now = answer.map(found -> found.tracks().getFirst().points()).orElse(last);
                        assertTrue(Math.max(farFrom(now, last), farFrom(last, now)) < 1.5, "stretch " + shape.segment() + " of " + shape.points()
                                + " at " + distance + ": " + last + " became " + now);
                        last = now;
                    }
                }
            }
        }
    }

    @Nested
    class AStretchPulledAgainstAnotherNetsTrack {

        // The inverting amplifier as the editor sets it out: a line of N under a long straight track of M, both from pad to pad.
        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N", "C", "2", "M", "D", "1", "M");
        private final List<Placement> placements = List.of(place("A", 0, 10), place("B", 20, 10), place("C", -5, 6.5), place("D", 25, 6.5));
        private final List<Point> under = line(1.5, 10, 4, 7.5, 16, 7.5, 18.5, 10);
        private final List<Point> over = line(-3.5, 6.5, 23.5, 6.5);

        @Test
        void neverComesNearerThanItsGapNorGoesRoundTheFarEndOfIt() {
            PcbDrawing pcb = pcb(placements, List.of(track(under), track(over)));
            RoutingBoard board = board(pcb, parts);
            RoutingEngine engine = new RoutingEngine(board);
            double asked = Polylines.lengthOf(under);
            for (double pull = 0.1; pull <= 4; pull += 0.1) {
                Optional<PcbDrawing> answer = engine.segmentCarried(pcb, 0, 1, 0, -pull, style(45, Tightening.JogHandling.MERGED), Optional.empty());
                if (answer.isEmpty()) {
                    continue;
                }
                for (int index : new int[] {0, 1}) {
                    assertClean(faultsOfTrack(board, answer.get(), index), "track " + index + ", pulled " + pull);
                }
                List<Point> line = answer.get().tracks().getFirst().points();
                assertTrue(Polylines.lengthOf(line) <= asked * SETTINGS.carrying().longestWalk() + 2 * pull, "pulled " + pull + ": " + line);
            }
        }
    }

    @Nested
    class ALineMadeOfSeveralTracksAsABoardReadFromAFileHasIt {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");
        private final List<Placement> placements = List.of(place("A", 0, 0), place("B", 21.5, 15));
        private final List<Track> tracks = List.of(track(line(1.5, 0, 1.5, 10), PcbVocabulary.TOP, 0.5),
                track(line(1.5, 10, 6.5, 15), PcbVocabulary.TOP, 0.4), track(line(20, 15, 6.5, 15), PcbVocabulary.TOP, 0.3));

        @Test
        void movesTheDiagonalParallelTheTracksEitherSideGrowingAndShrinkingEachKeepingItsOwnWidth() {
            PcbDrawing pcb = pcb(placements, tracks);
            PcbDrawing answer = new RoutingEngine(board(pcb, parts)).segmentCarried(pcb, 1, 0, -2, 2, WALKING, Optional.empty()).orElseThrow();
            assertLine(line(1.5, 0, 1.5, 14), answer.tracks().get(0).points());
            assertLine(line(1.5, 14, 2.5, 15), answer.tracks().get(1).points());
            assertLine(line(20, 15, 2.5, 15), answer.tracks().get(2).points());
            assertEquals(List.of(0.5, 0.4, 0.3), answer.tracks().stream().map(Track::width).toList());
        }

        @Test
        void movesItTheOtherWayAsWellAndAsFarAsItGoesWithoutAStep() {
            PcbDrawing pcb = pcb(placements, tracks);
            RoutingEngine engine = new RoutingEngine(board(pcb, parts));
            for (double distance : new double[] {0.5, 1, 1.5, 2, 3}) {
                PcbDrawing answer = engine.segmentCarried(pcb, 1, 0, distance, -distance, WALKING, Optional.empty()).orElseThrow();
                List<Point> third = Polylines.reversed(answer.tracks().get(2).points());
                List<Point> whole = Polylines.joined(answer.tracks().get(0).points(), answer.tracks().get(1).points().subList(1, answer.tracks().get(1).points().size()),
                        third.subList(1, third.size()));
                assertEquals(4, whole.size(), "" + whole);
                assertClean(faultsOfLine(whole), "by " + distance);
            }
        }

        @Test
        void carriesACornerWhereTwoTracksMeetAsACornerNotAsTheEndOfEither() {
            PcbDrawing pcb = pcb(placements, tracks);
            PcbDrawing answer = new RoutingEngine(board(pcb, parts)).cornerCarried(pcb, 0, 1, point(1.5, 12), WALKING);
            assertLine(List.of(point(1.5, 12)), List.of(answer.tracks().get(0).points().get(1)));
            assertLine(List.of(point(1.5, 12)), List.of(answer.tracks().get(1).points().getFirst()));
        }

        @Test
        void joinsTracksWhoseEndsOnlyMeetInTheirCopperAndTurnsWhereTheirOwnDirectionsMeet() {
            // LINEAR_500W_PS-1, D1's anode: the vertical ends at 83.966 44.819, the diagonal starts at 83.922 44.863.
            PcbDrawing pcb = pcb(List.of(place("A", 87.375, 49.816), place("B", 85.466, 25.671)),
                    List.of(track(line(83.966, 44.819, 83.966, 27.171), PcbVocabulary.TOP, 2.5), track(line(88.875, 49.816, 83.922, 44.863), PcbVocabulary.TOP, 2.5)));
            RoutingEngine engine = new RoutingEngine(board(pcb, nets("A", "1", "N", "A", "2", "N", "B", "1", "N", "B", "2", "N")));
            for (double distance : new double[] {0.5, 1, 2, 3}) {
                PcbDrawing answer = engine.segmentCarried(pcb, 1, 0, -distance, distance, WALKING, Optional.empty()).orElseThrow();
                List<Point> vertical = answer.tracks().get(0).points();
                List<Point> diagonal = answer.tracks().get(1).points();
                assertLine(List.of(diagonal.getLast()), List.of(vertical.getFirst()));
                assertEquals(83.966, vertical.getFirst().x(), 1e-3, "by " + distance + ": the vertical stays vertical");
                assertTrue(vertical.getFirst().y() > 44.9, "by " + distance + ": it grows down");
                assertTrue(diagonal.size() <= 4, "by " + distance + ": " + diagonal);
                List<Point> back = Polylines.reversed(diagonal);
                assertClean(faultsOfLine(Polylines.joined(Polylines.reversed(vertical), back.subList(1, back.size()))), "by " + distance);
            }
        }

        @Test
        void isNotJoinedAcrossAPadOrAVia() {
            PcbDrawing pcb = pcb(placements, tracks, List.of(new Via(1.5, 10, 0.3, 0.6, null, null)), List.of(), List.of());
            PcbDrawing answer = new RoutingEngine(board(pcb, parts)).segmentCarried(pcb, 1, 0, -2, 2, WALKING, Optional.empty()).orElseThrow();
            assertLine(line(1.5, 0, 1.5, 10), answer.tracks().get(0).points());
            assertLine(List.of(point(1.5, 10)), List.of(answer.tracks().get(1).points().getFirst()));
        }
    }

    @Nested
    class AJog {

        // A diagonal, a step back up of 0.3 hidden in the copper, then along: the owner's board.
        private final List<Point> jogged = line(0, 0, 10, 10, 10.3, 9.7, 20, 9.7);

        @Test
        void isKeptWhereItIsLongerThanTheTrackIsWide() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(jogged, PcbVocabulary.TOP, 0.2)));
            assertLine(jogged, new RoutingEngine(board(pcb, Map.of())).withoutJogs(pcb, List.of()).tracks().getFirst().points());
        }

        @Test
        void isMergedByADragOnlyWhenTheReaderAsksForIt() {
            // A jog one can see — 0.7 mm on a 1 mm track: longer than a step nobody means, shorter than a run.
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 10, 10.5, 9.5, 30, 9.5, 40.5, 20, 50, 20), PcbVocabulary.TOP, 1)));
            RoutingEngine engine = new RoutingEngine(board(pcb, Map.of()));
            PcbDrawing kept = engine.segmentCarried(pcb, 0, 3, 0, 1, WALKING, Optional.empty()).orElseThrow();
            PcbDrawing merged = engine.segmentCarried(pcb, 0, 3, 0, 1, style(45, Tightening.JogHandling.MERGED),
                    Optional.empty()).orElseThrow();
            assertLine(line(0, 0, 10, 10, 10.5, 9.5), kept.tracks().getFirst().points().subList(0, 3));
            assertEquals(kept.tracks().getFirst().points().size() - 1, merged.tracks().getFirst().points().size());
            assertClean(faultsOfLine(merged.tracks().getFirst().points()), "merged");
        }

        @Test
        void carriedWithinAJogOfTheRunBesideItLandsOnThatRun() {
            // A board as KiCad keeps it, one track a stretch: two flats a millimetre apart, a square step between. The lower one carried
            // up to a step's width short of the other — a step nobody means —: merged, it lands on the other's line, which stays.
            List<Track> tracks = List.of(track(line(-5, 5, 0, 0), PcbVocabulary.TOP, 0.2), track(line(0, 0, 10, 0), PcbVocabulary.TOP, 0.2),
                    track(line(10, 0, 10, -1), PcbVocabulary.TOP, 0.2), track(line(10, -1, 20, -1), PcbVocabulary.TOP, 0.2),
                    track(line(20, -1, 26, 5), PcbVocabulary.TOP, 0.2));
            Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");
            PcbDrawing pcb = pcb(List.of(place("A", -6.5, 5), place("B", 27.5, 5)), tracks);
            RoutingEngine engine = new RoutingEngine(board(pcb, parts));
            PcbDrawing kept = engine.segmentCarried(pcb, 1, 0, 0, -0.83, style(45, Tightening.JogHandling.KEPT), Optional.empty()).orElseThrow();
            PcbDrawing merged = engine.segmentCarried(pcb, 1, 0, 0, -0.83, style(45, Tightening.JogHandling.MERGED), Optional.empty()).orElseThrow();
            List<Point> keptLine = JoinedTracks.joinedLine(new RoutingEngine(board(kept, parts)).worldOf(kept), 1).points();
            List<Point> line = JoinedTracks.joinedLine(new RoutingEngine(board(merged, parts)).worldOf(merged), 1).points();
            assertTrue(keptLine.stream().anyMatch(point -> Math.abs(point.y() + 0.83) < 1e-6), "kept, the step stays where the hand left it: " + keptLine);
            for (int at = 1; at + 2 < line.size(); at++) {
                assertTrue(line.get(at).distanceTo(line.get(at + 1)) >= 0.4,
                        "a step of " + line.get(at).distanceTo(line.get(at + 1)) + " left in " + line);
            }
            assertTrue(line.stream().noneMatch(point -> Math.abs(point.y() + 0.83) < 1e-6), "merged, nothing stays a step off the run: " + line);
            assertTrue(line.stream().anyMatch(point -> Math.abs(point.y() + 1) < 1e-6 && Math.abs(point.x() - 20) < 1e-6), "the run beside it stays: " + line);
        }

        @Test
        void isMergedOnAFinishedBoardOnlyOnTheTracksAskedAndNeverIntoAnotherNetsCopper() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(jogged, PcbVocabulary.TOP, 1), track(line(30, 0, 40, 10, 40.3, 9.7, 50, 9.7), PcbVocabulary.TOP, 1)));
            PcbDrawing answer = new RoutingEngine(board(pcb, Map.of())).withoutJogs(pcb, List.of(0));
            assertLine(line(0, 0, 9.7, 9.7, 20, 9.7), answer.tracks().getFirst().points());
            assertSame(pcb.tracks().get(1), answer.tracks().get(1));
        }
    }

    @Nested
    class ASegmentCarried {

        @Test
        void staysShapelyWhereverItIsPulled() {
            record Shape(String name, List<Point> points, int segment) {
            }
            List<Shape> shapes = List.of(new Shape("a flat between diagonals", line(0, 0, 5, 5, 10, 5, 15, 10), 1),
                    new Shape("a long diagonal", line(0, 0, 0, 5, 20, 25, 20, 30), 1),
                    new Shape("a flat between square corners", line(0, 0, 0, 5, 20, 5, 20, 10), 1),
                    new Shape("the only stretch", line(0, 0, 20, 0), 0), new Shape("a first stretch", line(0, 0, 10, 0, 15, 5), 0),
                    new Shape("a last stretch", line(0, 0, 10, 0, 15, 5), 1));
            for (Shape shape : shapes) {
                PcbDrawing pcb = pcb(List.of(), List.of(track(shape.points())));
                RoutingBoard board = board(pcb, Map.of());
                RoutingEngine engine = new RoutingEngine(board);
                for (double[] way : RING) {
                    for (double distance : DISTANCES) {
                        Optional<PcbDrawing> answer = engine.segmentCarried(pcb, 0, shape.segment(), way[0] * distance, way[1] * distance, WALKING, Optional.empty());
                        if (answer.isPresent()) {
                            assertClean(faultsOfTrack(board, answer.get(), 0), shape.name() + " by " + way[0] * distance + ", " + way[1] * distance);
                            List<Point> line = answer.get().tracks().getFirst().points();
                            assertEquals(shape.points().getFirst(), line.getFirst());
                            assertEquals(shape.points().getLast(), line.getLast());
                        }
                    }
                }
            }
        }

        @Test
        void doesNotStopShortOfWhatIsInItsWayTheStretchGoesAcrossAsItIsPulledAndRoundIt() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 0, 5, 20, 25, 20, 30))), List.of(via(5, 18, 2)), List.of(), List.of());
            RoutingBoard board = board(pcb, Map.of());
            // Pulled 8 to the left, the diagonal goes 4√2 across itself: onto y = x + 13, where the via stands. It goes round the via the
            // shortest way, on its near side — how far across a line pulled onto something goes is the rubber band's to say, not a search's.
            PcbDrawing answer = new RoutingEngine(board).segmentCarried(pcb, 0, 1, -8, 0, WALKING, Optional.of(point(16, 21))).orElseThrow();
            assertClean(faultsOfTrack(board, answer, 0), "round the via");
            List<Point> line = answer.tracks().getFirst().points();
            boolean across = false;
            for (int index = 1; index < line.size(); index++) {
                Point point = line.get(index);
                Point was = line.get(index - 1);
                boolean diagonal = Math.abs((point.y() - point.x()) - (was.y() - was.x())) < 1e-6 && point.x() != was.x();
                across |= diagonal && point.y() - point.x() > 5 + 1e-3;
            }
            assertTrue(across, "the diagonal gone across itself, towards the pull: " + line);
        }

        @Test
        void neverRunsOverANeighboursCopperWalking() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 5, 0, 15, 0, 20, 0)), track(line(-30, 3, 50, 3))));
            RoutingBoard board = board(pcb, Map.of());
            RoutingEngine engine = new RoutingEngine(board);
            for (double[] way : RING) {
                for (double distance : DISTANCES) {
                    engine.segmentCarried(pcb, 0, 1, way[0] * distance, way[1] * distance, WALKING, Optional.empty())
                            .ifPresent(answer -> assertClean(faultsOfTrack(board, answer, 0), "by " + way[0] * distance + ", " + way[1] * distance));
                }
            }
        }

        @Test
        void gathersNoKnotsOverALongRunOfPulls() {
            Generator random = new Generator(12345);
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 0, 20, 10, 30, 10))), List.of(via(15, 15, 1.2)), List.of(), List.of());
            RoutingBoard board = board(pcb, Map.of());
            RoutingEngine engine = new RoutingEngine(board);
            PcbDrawing now = pcb;
            for (int pull = 0; pull < 60; pull++) {
                int segment = (int) Math.floor(random.next() * (now.tracks().getFirst().points().size() - 1));
                double dx = (random.next() - 0.5) * 6;
                double dy = (random.next() - 0.5) * 6;
                now = engine.segmentCarried(now, 0, segment, dx, dy, WALKING, Optional.empty()).orElse(now);
                assertClean(faultsOfTrack(board, now, 0), "pull " + pull);
                assertTrue(now.tracks().getFirst().points().size() <= 14, "pull " + pull + ": " + now.tracks().getFirst().points());
            }
        }
    }

    @Nested
    class AWholeRoutedBoardPartsDraggedAbout {

        private final Map<String, Map<String, String>> parts = nets("A", "1", "P", "A", "2", "Q", "B", "1", "Q", "B", "2", "R", "C", "1", "R", "C", "2", "S",
                "D", "1", "S", "D", "2", "T", "E", "1", "T", "E", "2", "P", "F", "1", "Q", "F", "2", "S");
        private final List<Placement> placements = List.of(place("A", 0, 0), place("B", 15, 0), place("C", 30, 0), place("D", 30, 15),
                place("E", 15, 15), place("F", 0, 15));

        @Test
        void laysThemAllWithinTheRules() {
            PcbDrawing pcb = pcb(placements, List.of());
            RoutingBoard board = board(pcb, parts);
            Autorouter.Routed answer = autorouted(board, pcb);
            assertEquals(List.of(), answer.unrouted());
            for (int index = 0; index < answer.pcb().tracks().size(); index++) {
                assertClean(faultsOfTrack(board, answer.pcb(), index), "track " + index);
            }
        }

        @Test
        void keepsEveryTrackShapelyAndClearAsPartAfterPartIsMoved() {
            PcbDrawing pcb = pcb(placements, List.of());
            RoutingBoard board = board(pcb, parts);
            RoutingEngine engine = new RoutingEngine(board);
            Generator random = new Generator(777);
            PcbDrawing now = autorouted(board, pcb).pcb();
            int notClear = 0;
            int refused = 0;
            for (int drag = 0; drag < 40; drag++) {
                int chosen = (int) Math.floor(random.next() * now.placements().size());
                Placement part = now.placements().get(chosen);
                // Moved as the editor moves a part: to the precision a point is kept to.
                double dx = Precision.settled((random.next() - 0.5) * 6);
                double dy = Precision.settled((random.next() - 0.5) * 6);
                // The part alone, as the editor says it: the copper it carries — a track between two of its pads goes whole — is the engine's.
                List<Placement> placed = new ArrayList<>(now.placements());
                placed.set(chosen, new Placement(part.designator(), Precision.settled(part.x() + dx), Precision.settled(part.y() + dy), 0,
                        PcbVocabulary.TOP, Optional.empty()));
                // A part let go where its copper cannot stand is refused, and stays where it was: a move not taken.
                switch (engine.tracksLaid(drag(now, now.withPlacements(placed), 45))) {
                    case RoutingEngine.Release.Laid laid -> now = laid.pcb();
                    case RoutingEngine.Release.Refused ignored -> refused += 1;
                }
                for (int index = 0; index < now.tracks().size(); index++) {
                    assertClean(faultsOfLine(now.tracks().get(index).points(), false), "drag " + drag + ", track " + index);
                    if (!faultsOfTrack(board, now, index, false).isEmpty()) {
                        notClear += 1;
                    }
                }
            }
            // Where parts are pushed onto each other's copper some must stand too near, and the checks say so; it must stay the exception.
            assertTrue(notClear < 60, "tracks too near other copper over all the drags: " + notClear);
            // Parts pushed onto each other at random cannot always stand: a move refused must stay the exception too.
            assertTrue(refused < 10, "moves refused over all the drags: " + refused);
        }
    }

    @Nested
    class ACornerCarried {

        @Test
        void staysShapelyWhereverItIsPulled() {
            Map<String, List<Point>> shapes = new LinkedHashMap<>();
            shapes.put("a square corner", line(0, 0, 30, 0, 30, 30));
            shapes.put("a flat run's corner", line(0, 0, 10, 0, 20, 10, 30, 10));
            shapes.put("a line's end", line(0, 0, 10, 0));
            shapes.forEach((name, points) -> {
                PcbDrawing pcb = pcb(List.of(), List.of(track(points)));
                RoutingBoard board = board(pcb, Map.of());
                RoutingEngine engine = new RoutingEngine(board);
                for (int corner = 0; corner < points.size(); corner++) {
                    for (double[] way : RING) {
                        for (double distance : DISTANCES) {
                            Point to = point(points.get(corner).x() + way[0] * distance, points.get(corner).y() + way[1] * distance);
                            PcbDrawing answer = engine.cornerCarried(pcb, 0, corner, to, WALKING);
                            // A line's own end is put where the pointer is: only what leads to it keeps to the angles.
                            boolean inner = corner != 0 && corner != points.size() - 1;
                            assertClean(faultsOfLine(answer.tracks().getFirst().points(), inner), "corner " + corner + " of " + name + " to " + to);
                            assertClean(faultsOfTrack(board, answer, 0, false), "corner " + corner + " of " + name + " to " + to);
                        }
                    }
                }
            });
        }
    }

    @Nested
    class AJogNobodyMeant {

        private final List<Point> stepped = line(0, 0, 10, 0, 12, 2, 12, 12, 12.1, 12.1, 12.1, 22, 14.1, 24, 30, 24);

        @Test
        void goesWhenTwoRunsOfOneDirectionStandALittleApartWhereTheReaderAsksJogsMerged() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(stepped)));
            RoutingBoard board = board(pcb, Map.of());
            PcbDrawing answer = new RoutingEngine(board).segmentCarried(pcb, 0, 6, 0, 1,
                    style(45, Tightening.JogHandling.MERGED), Optional.empty()).orElseThrow();
            List<Point> line = answer.tracks().getFirst().points();
            assertFalse(line.stream().anyMatch(point -> point.x() == 12.1), "" + line);
            assertClean(faultsOfTrack(board, answer, 0), "merged");
        }

        @Test
        void staysWhereTheReaderKeepsJogsNothingOfTheLineIsChangedThatTheyDidNotPull() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(stepped)));
            PcbDrawing answer = new RoutingEngine(board(pcb, Map.of())).segmentCarried(pcb, 0, 6, 0, 1, WALKING, Optional.empty()).orElseThrow();
            assertLine(stepped.subList(0, 6), answer.tracks().getFirst().points().subList(0, 6));
        }
    }

    @Test
    void aTrackBeingLaidGoesRoundAWallWithAWayRoundItAndStopsAtOneWithNone() {
        PcbDrawing pcb = pcb(List.of(), List.of(track(line(-3, 5, 13, 5))));
        RoutingBoard board = board(pcb, Map.of());
        TrackRouter.RoutedLeg round = new RoutingEngine(board).leg(pcb, OptionalInt.empty(), PcbVocabulary.TOP, Optional.of("A"), 0.25, point(5, 0), point(5, 10), 45, 0).leg();
        assertTrue(round.reached());
        List<Track> tracks = new ArrayList<>(pcb.tracks());
        tracks.add(track(Polylines.joined(List.of(point(5, 0)), round.points())));
        assertClean(faultsOfTrack(board, pcb.withTracks(tracks), 1), "the leg laid");
        PcbDrawing walled = pcb(List.of(), List.of(track(line(-100, 5, 100, 5))));
        assertFalse(new RoutingEngine(board(walled, Map.of())).leg(walled, OptionalInt.empty(), PcbVocabulary.TOP, Optional.of("A"), 0.25, point(5, 0), point(5, 10), 45, 0).leg().reached());
    }

    /**
     * A track laid on a slant out of a thin pad's middle, between neighbours at a fine pitch, leaves it straight along the pad, then
     * turns — as KiCad's smart pads lay it —, never out over its corner towards its neighbour; laid out of it at the pad's own angle,
     * it is left as it is.
     */
    @Test
    void aTrackLeavesAThinPadAlongItNotOverItsCorner() {
        List<FootprintDrawing.Pad> row = new ArrayList<>();
        for (int pin = -1; pin <= 1; pin++) {
            row.add(new FootprintDrawing.Pad(String.valueOf(pin + 2), "surface", "rectangle", pin * 0.65, 0, 0, 0.3, 1.5, 0, 0, 0, "", "", "", null, null));
        }
        FootprintDrawing thin = new FootprintDrawing(row, List.of(), List.of(), null, List.of());
        PcbDrawing pcb = pcb(List.of(place("T", 0, 0)), List.of());
        RoutingBoard rowBoard = board(pcb, nets("T", "1", "B", "T", "2", "A", "T", "3", "C"), thin);
        RoutingEngine engine = new RoutingEngine(rowBoard);
        List<Point> slanted = engine.leg(pcb, OptionalInt.empty(), PcbVocabulary.TOP, Optional.of("A"), 0.2, point(0, 0), point(5, -5), 45, 0).leg().points();
        // Straight down along the pad before it turns, clear of its neighbours.
        assertEquals(0, slanted.getFirst().x(), Precision.SAME_POINT, "leaves along the pad: " + slanted);
        assertLine(line(5, -5), List.of(slanted.getLast()));
        PcbDrawing laid = pcb.withTracks(List.of(track(Polylines.joined(List.of(point(0, 0)), slanted), PcbVocabulary.TOP, 0.2)));
        assertClean(faultsOfTrack(rowBoard, laid, 0), "out between its neighbours");
        List<Point> along = engine.leg(pcb, OptionalInt.empty(), PcbVocabulary.TOP, Optional.of("A"), 0.2, point(0, 0), point(0, -5), 45, 0).leg().points();
        assertLine(line(0, -5), along);
        // A thin pad standing alone, nothing in the way: the way found runs on a slant straight out of its middle, over its corner; it
        // is laid out of the pad's side instead.
        FootprintDrawing alone = new FootprintDrawing(List.of(row.get(1)), List.of(), List.of(), null, List.of());
        RoutingEngine single = new RoutingEngine(board(pcb, nets("T", "2", "A"), alone));
        List<Point> out = single.leg(pcb, OptionalInt.empty(), PcbVocabulary.TOP, Optional.of("A"), 0.2, point(0, 0), point(5, -5), 45, 0).leg().points();
        Point first = out.getFirst();
        assertTrue(Math.abs(first.x()) < Precision.SAME_POINT || Math.abs(first.y()) < Precision.SAME_POINT, "out of a side, square to it: " + out);
    }

    /**
     * A track begun on bare pcb is on no net, and a leg laid on from its end is of that track, not in its way: on 2026-11-15 a track
     * begun off any pad stopped at its first corner, every leg after it refused — its own copper the copper of another net.
     */
    @Test
    void aTrackOnNoNetIsCarriedOnFromItsEndItsOwnCopperNotInItsWay() {
        PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 5, 0))));
        RoutingEngine engine = new RoutingEngine(board(pcb, Map.of()));
        TrackRouter.RoutedLeg carriedOn = engine.leg(pcb, OptionalInt.of(0), PcbVocabulary.TOP, Optional.empty(), 0.25, point(5, 0), point(10, 5), 45, 0).leg();
        assertTrue(carriedOn.reached(), "laid on from its own end");
        assertLine(line(10, 5), List.of(carriedOn.points().getLast()));
    }

    /**
     * A stretch pulled onto a via, taken by a point well off it: it is caught on the via's near side, as a band pulled over a peg, and
     * goes round under it — never hopping over to its far side, though that is the shorter way round. On 2026-11-15 the way round was
     * the shortest, and a stretch pulled a little onto a via jumped across it.
     */
    @Test
    void aStretchPulledOntoAViaIsCaughtOnItsNearSide() {
        PcbDrawing pcb = pcb(List.of(), List.of(track(line(-10, 0, 0, 0, 20, 0, 30, 0))), List.of(via(10, 1, 0.6)), List.of(), List.of());
        RoutingBoard board = board(pcb, Map.of());
        PcbDrawing answer = new RoutingEngine(board).segmentCarried(pcb, 0, 1, 0, 1.3, WALKING, Optional.of(point(4, 0))).orElseThrow();
        List<Point> line = answer.tracks().getFirst().points();
        double under = Double.POSITIVE_INFINITY;
        for (int index = 1; index < line.size(); index++) {
            Point from = line.get(index - 1);
            Point to = line.get(index);
            if (Math.min(from.x(), to.x()) <= 10 && Math.max(from.x(), to.x()) >= 10) {
                double share = from.x() == to.x() ? 0 : (10 - from.x()) / (to.x() - from.x());
                under = Math.min(under, from.y() + (to.y() - from.y()) * share);
            }
        }
        assertTrue(under < 1, "round under the via, on the side it came from: " + line);
        assertTrue(line.stream().anyMatch(point -> point.y() > 1.2), "pulled up either side of it: " + line);
        assertClean(faultsOfTrack(board, answer, 0), "the stretch pulled");
    }

    /**
     * A wall of another net's copper with its only gap far off to one side: searched through the corridor of free room, the leg goes
     * all the way round through the gap and is laid clean at 45°. On 2026-11-15 a track drawn round a connector to a switch far below
     * found no way by either search the reader had then — a grid near the straight line, a walk round what it met —, both gone since.
     */
    @Test
    void aTrackBeingLaidFindsAWayFarRoundAWallThroughTheCorridor() {
        PcbDrawing pcb = pcb(List.of(), List.of(track(line(-100, 5, 30, 5)), track(line(34, 5, 100, 5))));
        RoutingBoard board = board(pcb, Map.of());
        RoutingEngine engine = new RoutingEngine(board);
        TrackRouter.RoutedLeg corridor = engine.leg(pcb, OptionalInt.empty(), PcbVocabulary.TOP, Optional.of("A"), 0.25, point(5, 0), point(5, 10), 45, 0).leg();
        assertTrue(corridor.reached(), "round through the gap: " + corridor.points());
        List<Track> tracks = new ArrayList<>(pcb.tracks());
        tracks.add(track(Polylines.joined(List.of(point(5, 0)), corridor.points())));
        assertClean(faultsOfTrack(board, pcb.withTracks(tracks), 2), "the leg laid round the wall");
    }

    /**
     * A leg is found with sharp corners, and drawn with the reader's: a corner rounded inward comes nearer what stands inside it.
     * On lm317-supply a leg turned down past RV1's pad 1 with 0.45 mm to spare as found and 0.04 mm as drawn with 1 mm corners —
     * the checks said so on 2026-11-12. The corners it is answered with are those that keep its gaps.
     */
    @Test
    void aTrackBeingLaidIsDrawnOnlyWithCornersThatKeepItsGaps() {
        // A via inside the corner a leg turns at, clear of both its runs and in the way of a corner rounded by 4 mm.
        PcbDrawing pcb = pcb(List.of(), List.of(), List.of(new Via(8.8, 1.2, 0.2, 0.4, PcbVocabulary.TOP, PcbVocabulary.BOTTOM)), List.of(), List.of());
        RoutingBoard board = board(pcb, Map.of());
        RoutingEngine.LaidLeg laid = new RoutingEngine(board).leg(pcb, OptionalInt.empty(), PcbVocabulary.TOP, Optional.of("A"), 0.25, point(0, 0), point(10, 10), 90, 4);
        assertLine(line(10, 0, 10, 10), laid.leg().points());
        assertTrue(laid.leg().reached());
        assertTrue(laid.rounding() < 4, "corners of 4 mm would run over the via: " + laid.rounding());
        List<Track> tracks = new ArrayList<>(pcb.tracks());
        tracks.add(new Track(PcbVocabulary.TOP, 0.25, laid.rounding(), Polylines.joined(List.of(point(0, 0)), laid.leg().points())));
        RoutingWorld world = new RoutingWorld(pcb.withTracks(tracks), board);
        assertTrue(world.fence(PcbVocabulary.TOP, Optional.of("A"), 0.25, OptionalInt.of(0)).copperClear(
                CopperShapes.track(tracks.getLast(), CURVE_STEPS)), "drawn as answered, it keeps its gaps");
    }

    @Nested
    class TheReadersSettings {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");

        private List<Double> angles(Track line) {
            List<Double> angles = new ArrayList<>();
            for (int index = 1; index < line.points().size(); index++) {
                Point from = line.points().get(index - 1);
                Point to = line.points().get(index);
                angles.add(Math.toDegrees(Math.atan2(to.y() - from.y(), to.x() - from.x())));
            }
            return angles;
        }

        private Autorouter.Routed route(double angleStep, double rounding, List<Track> tracks, List<NetClass> netClasses) {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 7)), tracks, List.of(), List.of(), netClasses);
            return autorouted(board(pcb, parts), pcb, angleStep, rounding, Autorouter.Crossing.VIAS, moment -> {
            });
        }

        @Test
        void laysAtRightAnglesWhenAsked() {
            Autorouter.Routed answer = route(90, 0, List.of(), List.of());
            assertEquals(List.of(), answer.unrouted());
            for (Track line : answer.pcb().tracks()) {
                for (double angle : angles(line)) {
                    assertTrue(Math.abs(angle / 90 - Math.round(angle / 90)) < 0.001, "" + line.points());
                }
            }
        }

        @Test
        void laysAt45OtherwiseAndWithTheCornersAskedFor() {
            Autorouter.Routed answer = route(45, 0.5, List.of(), List.of());
            assertTrue(answer.pcb().tracks().stream().allMatch(line -> line.rounding() == 0.5));
            assertTrue(answer.pcb().tracks().stream().flatMap(line -> angles(line).stream()).anyMatch(angle -> Math.abs(angle / 90 - Math.round(angle / 90)) > 0.1));
        }

        @Test
        void takesANetClassesWidthAndKeepsItsClearance() {
            List<NetClass> classes = List.of(new NetClass("power", 0.5, 0.4, 0, 0, List.of("N", "W")));
            Autorouter.Routed answer = route(45, 0, List.of(track(line(-100, 3.2, 3, 3.2, 3, 100))), classes);
            List<Track> laid = answer.pcb().tracks().subList(1, answer.pcb().tracks().size());
            assertFalse(laid.isEmpty());
            assertTrue(laid.stream().allMatch(line -> line.width() == 0.5));
            RoutingWorld world = new RoutingWorld(answer.pcb(), board(answer.pcb(), parts));
            TrackFence fence = world.fence(PcbVocabulary.TOP, Optional.of("N"), 0.5);
            for (Track line : laid) {
                assertTrue(fence.lineClear(line.points()), "" + line.points());
            }
            assertEquals(0.4, world.clearanceOf(Optional.of("N")));
        }

        @Test
        void carriesASegmentAtRightAnglesAsTracksOfRightAnglesWant() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 0, 10, 10, 20, 10))));
            PcbDrawing answer = new RoutingEngine(board(pcb, Map.of())).segmentCarried(pcb, 0, 0, 0, 3,
                    style(90, Tightening.JogHandling.KEPT), Optional.empty()).orElseThrow();
            assertClean(faultsOfLine(answer.tracks().getFirst().points()), "at right angles");
            for (double angle : angles(answer.tracks().getFirst())) {
                assertTrue(Math.abs(angle / 90 - Math.round(angle / 90)) < 0.001, "" + answer.tracks().getFirst().points());
            }
        }
    }

    @Nested
    class TheAutorouter {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");

        @Test
        void takesASurfacePadOnlyOnItsOwnSide() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 0)), List.of());
            Autorouter.Routed answer = autorouted(board(pcb, parts), pcb);
            assertEquals(List.of(), answer.unrouted());
            assertTrue(answer.pcb().tracks().stream().allMatch(line -> line.layer().equals(PcbVocabulary.TOP)));
            assertEquals(List.of(), answer.pcb().vias());
        }

        @Test
        void goesThroughViasUnderAWallComingBackToThePadsSide() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 0, 12)), List.of(track(line(-200, 6, 200, 6))));
            Autorouter.Routed answer = autorouted(board(pcb, parts), pcb);
            assertEquals(List.of(), answer.unrouted());
            assertEquals(2, answer.pcb().vias().size());
            List<Track> laid = answer.pcb().tracks().subList(1, answer.pcb().tracks().size());
            assertTrue(laid.stream().anyMatch(line -> line.layer().equals(PcbVocabulary.BOTTOM)));
            for (Point end : line(1.5, 0, 1.5, 12, -1.5, 12)) {
                assertTrue(laid.stream().filter(line -> line.points().stream().anyMatch(point -> point.distanceTo(end) < 0.001))
                        .allMatch(line -> line.layer().equals(PcbVocabulary.TOP)));
            }
        }

        @Test
        void crossesByALinkWhereNothingElseGoesAndOnlyWhenAllowed() {
            // A wall on both layers: no via gets past it, only a part standing over the top one.
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 0, 12)), List.of(track(line(-300, 6, 300, 6)),
                    track(line(-300, 6, 300, 6), PcbVocabulary.BOTTOM, 0.25)));
            RoutingBoard board = board(pcb, parts);
            Autorouter.Routed refused = autorouted(board, pcb, 45, 0, Autorouter.Crossing.VIAS, moment -> {
            });
            assertEquals(1, refused.unrouted().size());
            Autorouter.Routed linked = autorouted(board, pcb, 45, 0, Autorouter.Crossing.LINKS, moment -> {
            });
            List<Placement> links = linked.pcb().placements().stream().filter(placement -> placement.footprint().isPresent()).toList();
            assertEquals(List.of(), linked.unrouted());
            assertEquals(1, links.size());
            assertEquals(Map.of("1", "N", "2", "N"), links.getFirst().nets());
        }

        /**
         * A via is never drilled into another hole, its own net's among them: on 2026-11-15 the autorouter, searching through the
         * corridor, put a via of a net straight on one of the same net's, its copper no obstacle — and the checks found two holes 0 mm
         * apart.
         */
        @Test
        void aViaKeepsItsHoleClearOfEveryHoleItsOwnNetsAmongThem() {
            PcbDrawing pcb = pcb(List.of(), List.of(), List.of(via(5, 5, 0.6)), List.of(), List.of());
            RoutingWorld world = new RoutingEngine(board(pcb, parts)).worldOf(pcb);
            TrackFence holes = world.holeFence(0.3);
            double keep = world.rules().value(PcbVocabulary.RULE_HOLE_CLEARANCE);
            assertFalse(holes.pointClear(point(5, 5)), "on the via");
            assertFalse(holes.pointClear(point(5 + 0.2 + keep / 2, 5)), "the holes overlapping, or nearer than the hole clearance");
            assertTrue(holes.pointClear(point(5 + 0.3 + keep + 0.01, 5)), "a hole's gap off");
        }

        @Test
        void isToldEveryMoment() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 0)), List.of());
            List<Autorouter.Step> steps = new ArrayList<>();
            autorouted(board(pcb, parts), pcb, 45, 0, Autorouter.Crossing.VIAS, moment -> steps.add(moment.step()));
            assertTrue(steps.contains(Autorouter.Step.TRYING));
            assertTrue(steps.contains(Autorouter.Step.LAID));
        }
    }

    @Nested
    class APlane {

        private final Zone ground = new Zone(PcbVocabulary.BOTTOM, "0", 0.4, 0, "M-20 -20H20V20H-20Z");

        @Test
        void takesTheSurfacePadsOfItsNetOnTheOtherSideEachByADropOfItsOwn() {
            PcbDrawing pcb = pcb(List.of(place("A", -8, 0), place("B", 8, 0)), List.of(), List.of(), List.of(ground), List.of());
            Autorouter.Routed answer = autorouted(board(pcb, nets("A", "1", "0", "B", "2", "0")), pcb);
            assertEquals(List.of(), answer.unrouted());
            assertEquals(2, answer.pcb().vias().size());
            assertEquals(2, answer.pcb().tracks().size());
            for (Track drop : answer.pcb().tracks()) {
                assertTrue(Polylines.lengthOf(drop.points()) < SETTINGS.autorouting().planeDrops().reach());
            }
        }

        @Test
        void leavesAPadItsCopperAlreadyJoinsToThePlane() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0)), List.of(track(line(-1.5, 0, -1.5, 3))), List.of(via(-1.5, 3, 0.6)), List.of(ground), List.of());
            assertEquals(1, autorouted(board(pcb, nets("A", "1", "0")), pcb).pcb().vias().size());
        }

        @Test
        void dropsNothingOfANetItIsNotPouredWith() {
            PcbDrawing pcb = pcb(List.of(place("A", -8, 0), place("B", 8, 0)), List.of(), List.of(), List.of(ground), List.of());
            assertEquals(List.of(), autorouted(board(pcb, nets("A", "1", "N", "B", "2", "N")), pcb).pcb().vias());
        }
    }

    @Nested
    class AStretchPulledAcrossPastAColumnOfPads {

        private final Map<String, Map<String, String>> parts = nets("P1", "1", "mains", "J1", "1", "mains", "Q2", "1", "two", "Q3", "1", "three");
        private final PcbDrawing pcb = pcb(List.of(place("P1", 16.5, 35.5), place("J1", 5.5, 25.5), place("Q2", 16.5, 30.5), place("Q3", 16.5, 25.5)),
                List.of(track(line(16.5, 35.5, 6.5, 25.5, 5.5, 25.5))));
        private final RoutingBoard board = board(pcb, parts, PIN);
        private final RoutingEngine engine = new RoutingEngine(board);

        private PcbDrawing pulled(double by) {
            PcbDrawing shown = pcb;
            for (int frame = 1; frame <= 12; frame++) {
                shown = engine.segmentCarried(pcb, 0, 0, by * frame / 12, -by * frame / 12, WALKING, Optional.of(point(11.5, 30.5))).orElse(shown);
            }
            return shown;
        }

        @Test
        void hugsTheColumnOnTheSideItCameFromNeverSlippingIntoTheGapBetweenItsPads() {
            for (double by : new double[] {4, 6, 9, 12}) {
                PcbDrawing shown = pulled(by);
                List<Point> line = shown.tracks().getFirst().points();
                assertClean(faultsOfTrack(board, shown, 0), "by " + by);
                // Left of both pads' copper and gaps: 16.5 less the pad, the gap and half the track.
                double rightmost = line.subList(1, line.size() - 1).stream().mapToDouble(Point::x).max().orElse(0);
                assertTrue(rightmost <= 16.5 - 1.15 - 0.2 - 0.125 + 1e-3, "" + line);
                // It was pulled: it presses on the column rather than lying where it was.
                assertTrue(Polylines.lengthOf(line) > Polylines.lengthOf(pcb.tracks().getFirst().points()) + 1);
            }
        }

        @Test
        void staysAsItIsOnceItPressesOnTheColumnHoweverMuchFurtherItIsPulled() {
            assertLine(pulled(6).tracks().getFirst().points(), pulled(12).tracks().getFirst().points());
        }

        @Test
        void leavesNoStepShorterThanOneAPersonCanMean() {
            for (double by : new double[] {1, 2, 3, 4, 6}) {
                List<Point> line = pulled(by).tracks().getFirst().points();
                for (int index = 2; index < line.size() - 1; index++) {
                    assertTrue(line.get(index - 1).distanceTo(line.get(index)) >= 0.25 * SETTINGS.routing().leastStep(), "" + line);
                }
            }
        }
    }

    @Test
    void aStepNobodyCanMeanGoesFromALineWhateverBecomesOfJogs() {
        PcbDrawing pcb = pcb(List.of(), List.of());
        TrackFence fence = new RoutingWorld(pcb, board(pcb, Map.of())).fence(PcbVocabulary.TOP, Optional.empty(), 0.25);
        Tightening tightening = new Tightening(fence, 45, List.of(), SETTINGS.routing());
        assertLine(line(52.5, 22.78, 60.85, 22.78, 60.88, 22.75),
                tightening.untangled(line(52.5, 22.78, 59.85, 22.78, 59.88, 22.75, 60.88, 22.75), Tightening.JogHandling.KEPT));
        // A jog one can see is the reader's, kept.
        List<Point> seen = line(0, 0, 10, 0, 10.5, 0.5, 20, 0.5);
        assertLine(seen, tightening.untangled(seen, Tightening.JogHandling.KEPT));
    }

    @Nested
    class CornersRounded {

        @Test
        void aLineOfStretchesChosenIsOneTrackRoundedAtItsJointsTheChoiceKeptOnIt() {
            // As KiCad keeps a line: one track a stretch. All three chosen, rounded: one track, its corners its own, the choice on it.
            List<Track> tracks = List.of(track(line(-5, 5, 0, 0), PcbVocabulary.TOP, 0.2), track(line(0, 0, 10, 0), PcbVocabulary.TOP, 0.2),
                    track(line(10, 0, 15, 5), PcbVocabulary.TOP, 0.2), track(line(30, 30, 40, 30), PcbVocabulary.TOP, 0.2));
            Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");
            PcbDrawing pcb = pcb(List.of(place("A", -6.5, 5), place("B", 16.5, 5)), tracks);
            CornerRounding.Rounded rounded = new RoutingEngine(board(pcb, parts)).cornersRounded(pcb, 1, List.of(0, 1, 2));
            assertEquals(2, rounded.pcb().tracks().size());
            assertLine(line(-5, 5, 0, 0, 10, 0, 15, 5), rounded.pcb().tracks().getFirst().points());
            assertTrue(rounded.pcb().tracks().getFirst().rounding() > 0, "rounded at its joints");
            assertEquals(List.of(0), rounded.chosen());
            assertSame(pcb.tracks().get(3), rounded.pcb().tracks().get(1));
        }

        @Test
        void aStretchChosenIsItsLineChosen() {
            List<Track> tracks = List.of(track(line(-5, 5, 0, 0), PcbVocabulary.TOP, 0.2), track(line(0, 0, 10, 0), PcbVocabulary.TOP, 0.2),
                    track(line(10, 0, 15, 5), PcbVocabulary.TOP, 0.2));
            Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");
            PcbDrawing pcb = pcb(List.of(place("A", -6.5, 5), place("B", 16.5, 5)), tracks);
            CornerRounding.Rounded rounded = new RoutingEngine(board(pcb, parts)).cornersRounded(pcb, 1, List.of(1));
            assertEquals(1, rounded.pcb().tracks().size());
            assertEquals(List.of(0), rounded.chosen());
        }

        @Test
        void stretchesOfAnotherWidthStayTracksOfTheirOwn() {
            List<Track> tracks = List.of(track(line(-5, 5, 0, 0), PcbVocabulary.TOP, 0.2), track(line(0, 0, 10, 0), PcbVocabulary.TOP, 0.5),
                    track(line(10, 0, 15, 5), PcbVocabulary.TOP, 0.2));
            Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");
            PcbDrawing pcb = pcb(List.of(place("A", -6.5, 5), place("B", 16.5, 5)), tracks);
            CornerRounding.Rounded rounded = new RoutingEngine(board(pcb, parts)).cornersRounded(pcb, 1, List.of(0, 1, 2));
            assertEquals(3, rounded.pcb().tracks().size());
            assertEquals(List.of(0, 1, 2), rounded.chosen());
        }

        @Test
        void everyCornerTakesTheRadiusAskedWhateverStandsNear() {
            // Copper inside the second turn: the corners are rounded as asked all the same — the gap is the checks' to say, the reader's to make.
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 0, 10, 10, 20, 10), PcbVocabulary.TOP, 0.2),
                    track(line(11, 9, 11.1, 8.9), PcbVocabulary.TOP, 0.2)));
            Track rounded = new RoutingEngine(board(pcb, Map.of())).cornersRounded(pcb, 3, List.of(0)).pcb().tracks().getFirst();
            assertEquals(3, rounded.rounding());
            assertEquals(List.of(), rounded.corners(), "every corner keeps the radius asked");
        }

        @Test
        void aShortLegHoldsNoOtherLineBack() {
            // A long line and a short one chosen together, 3 asked: both are asked 3 — the short one's legs alone cut its own corner.
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 50, 0, 50, 50), PcbVocabulary.TOP, 0.2),
                    track(line(0, 20, 0.4, 20, 0.4, 20.4), PcbVocabulary.TOP, 0.2)));
            List<Track> rounded = new RoutingEngine(board(pcb, Map.of())).cornersRounded(pcb, 3, List.of(0, 1)).pcb().tracks();
            assertEquals(3, rounded.get(0).rounding(), 1e-9);
            assertEquals(3, rounded.get(1).rounding(), 1e-9);
            assertEquals(List.of(), rounded.get(0).corners());
        }

        @Test
        void aTrackMovedKeepsItsCornersOrTheLeastOfThem() {
            Track track = track(line(0, 0, 10, 0, 10, 10, 20, 10), PcbVocabulary.TOP, 0.2).withCorners(2, List.of(2.0, 0.5));
            assertEquals(List.of(2.0, 0.5), track.withPoints(line(0, 1, 10, 1, 10, 11, 20, 11)).corners());
            assertEquals(List.of(0.5), track.withPoints(line(0, 0, 10, 0, 10, 10)).corners());
            assertEquals(List.of(), track.withCorners(2, List.of(2.0, 2.0)).corners());
        }
    }

    /**
     * The magnet on, a track carried along another of its net within the gap becomes one with it: it runs into the other's line and
     * ends there, a T; where it leaves the line again it goes on as a track of its own.
     */
    @Nested
    class MergedAsCarried {

        private final Map<String, Map<String, String>> parts = nets("A", "1", "N", "B", "1", "N", "C", "1", "N", "D", "1", "N");

        /** A from pad to pad along y = 0; B a U of the same net whose middle runs 0.6 above it. */
        private final PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 5, -5), place("C", 20, 0), place("D", 11.5, -5)),
                List.of(track(line(-1.5, 0, 18.5, 0)), track(line(3.5, -5, 3.5, 0.6, 10, 0.6, 10, -5))));

        private PcbDrawing carriedDown(double by, OptionalDouble merging) {
            TrackRouter.LayingStyle magnet = new TrackRouter.LayingStyle(45, Tightening.JogHandling.KEPT, merging);
            return new RoutingEngine(board(pcb, parts)).segmentCarried(pcb, 1, 1, 0, -by, magnet, Optional.empty()).orElseThrow();
        }

        @Test
        void carriedWithinTheGapItEndsOnTheOtherLineTwice() {
            PcbDrawing merged = carriedDown(0.5, OptionalDouble.of(0.3));
            assertEquals(3, merged.tracks().size(), "B leaves A's line again on the other side: two tracks of it");
            assertLine(line(3.5, -5, 3.5, 0), merged.tracks().get(1).points());
            assertLine(line(10, 0, 10, -5), merged.tracks().get(2).points());
        }

        @Test
        void carriedFartherThanTheGapItStaysWhole() {
            assertEquals(2, carriedDown(0.2, OptionalDouble.of(0.3)).tracks().size());
        }

        @Test
        void theMagnetOffMergesNothing() {
            assertEquals(2, carriedDown(0.5, OptionalDouble.empty()).tracks().size());
        }

        @Test
        void aJogLeftAtTheJointOfWhatIsCarriedIsMergedToo() {
            // Two diagonals one beside the other, joined by a step 0.15 long at their joint; the run after them carried a hair, merging on.
            PcbDrawing jogged = pcb(List.of(), List.of(track(line(-10, 0, 0, 0, 5, 5)), track(line(5, 5, 5, 5.15, 10, 10.15, 20, 10.15))));
            TrackRouter.LayingStyle merging = new TrackRouter.LayingStyle(45, Tightening.JogHandling.MERGED, OptionalDouble.of(0.3));
            PcbDrawing carried = new RoutingEngine(board(jogged, Map.of())).segmentCarried(jogged, 1, 2, 0, 0.05, merging, Optional.empty())
                    .orElseThrow();
            // Between the line's two free ends — the far one carried along with the run, a hair of it left there.
            List<Point> points = carried.tracks().stream().flatMap(track -> track.points().stream()).toList();
            for (int at = 2; at < points.size() - 1; at++) {
                double leg = points.get(at - 1).distanceTo(points.get(at));
                assertTrue(leg < 1e-6 || leg > 0.3, "no step is left: " + carried.tracks().stream().map(Track::points).toList());
            }
        }
    }

    /** A jog is the line's, not a track's: one standing at the joint of two tracks is merged as one inside a track is. */
    @Nested
    class JogsOfALine {

        @Test
        void aJogAtTheJointOfTwoTracksIsMerged() {
            // The first track runs to the joint at 10, 0; the second steps 0.2 aside there and runs on beside the first's line.
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 0)), track(line(10, 0, 10.2, 0.2, 20, 0.2))));
            PcbDrawing merged = new RoutingEngine(board(pcb, Map.of())).withoutJogs(pcb, List.of(0));
            List<Point> points = merged.tracks().stream().flatMap(track -> track.points().stream()).toList();
            assertTrue(points.stream().noneMatch(point -> point.x() > 1 && point.x() < 19 && Math.abs(point.y()) > 1e-6),
                    "no step is left between the ends: " + merged.tracks().stream().map(Track::points).toList());
        }

        @Test
        void twoTracksRunningStraightOnThroughAJointAreOne() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 5, 5)), track(line(5, 5, 10, 10, 20, 10))));
            PcbDrawing merged = new RoutingEngine(board(pcb, Map.of())).withoutJogs(pcb, List.of(0));
            assertEquals(1, merged.tracks().size(), "one conductor: " + merged.tracks().stream().map(Track::points).toList());
            assertLine(line(0, 0, 10, 10, 20, 10), merged.tracks().getFirst().points());
        }

        @Test
        void aKnotOfASliverAtTheJointOfTwoTracksIsUntied() {
            // As an import leaves it: one track runs down to B, a sliver 0.053 long goes on from B to A, and the third turns back
            // from A — a knot at the joint, the line doubling back on itself, nobody's meaning.
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(162.888, 38.1, 162.888, 39.862)), track(line(162.888, 39.862, 162.925, 39.9)),
                    track(line(162.925, 39.9, 150, 39.9))));
            PcbDrawing merged = new RoutingEngine(board(pcb, Map.of())).withoutJogs(pcb, List.of(0));
            List<Point> points = merged.tracks().stream().flatMap(track -> track.points().stream()).toList();
            assertTrue(points.stream().noneMatch(point -> point.x() > 162.888 + 1e-6),
                    "nothing doubles back past the vertical: " + merged.tracks().stream().map(Track::points).toList());
        }
    }

    /** A track left standing too near another — a conflict its reader made — is carried out of it, never deeper into it. */
    @Nested
    class OutOfAConflict {

        /** Two tracks 0.3 apart, centre to centre: at 0.25 wide, 0.05 of copper between them, far under any gap. */
        private PcbDrawing tooNear() {
            return pcb(List.of(), List.of(track(line(0, 0, 10, 0, 20, 0, 30, 0)), track(line(0, 0.3, 30, 0.3))));
        }

        @Test
        void aTrackInCopperJoiningTwoNetsIsCarriedLikeAnyOther() {
            // A's two pads are on two nets, and the two tracks join them: a short, so neither track has a net. The copper each is joined
            // to is still its own — the other track at their joint, the pads it ends on —, never what it must keep clear of.
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0)), List.of(track(line(-1.5, 0, -1.5, -5, 5, -5)), track(line(5, -5, 5, 0, 1.5, 0))));
            PcbDrawing answer = new RoutingEngine(board(pcb, nets("A", "1", "N1", "A", "2", "N2"))).segmentCarried(pcb, 0, 1, 0, -2, WALKING, Optional.empty())
                    .orElseThrow(() -> new AssertionError("the track is held by the copper it is joined to"));
            double highest = answer.tracks().getFirst().points().stream().mapToDouble(Point::y).min().orElseThrow();
            assertTrue(highest < -6, "the stretch moved: " + answer.tracks().getFirst().points());
        }

        private Optional<PcbDrawing> carried(PcbDrawing pcb, double dy) {
            return new RoutingEngine(board(pcb, Map.of())).segmentCarried(pcb, 0, 1, 0, dy, WALKING, Optional.empty());
        }

        @Test
        void aStretchStandingTooNearIsNeverCarriedNearer() {
            PcbDrawing pcb = tooNear();
            Optional<PcbDrawing> answer = carried(pcb, 0.2);
            double highest = answer.orElse(pcb).tracks().getFirst().points().stream().mapToDouble(Point::y).max().orElseThrow();
            assertTrue(highest <= 1e-9, "the stretch came nearer than it stood: " + answer.map(drawing -> drawing.tracks().getFirst().points()));
        }
    }

    /** A track held where it stands — locked itself, or in a locked group —: no act of the router moves or reshapes it. */
    @Nested
    class HeldWhereItStands {

        private static final Arrangement LOCKED = new Arrangement("", DrawnState.LOCKED);

        private PcbDrawing withLockedTrack() {
            return pcb(List.of(), List.of(track(line(0, 0, 10, 0, 20, 10)).arranged(LOCKED)));
        }

        @Test
        void aLockedTrackIsNeitherCarriedNorReshaped() {
            PcbDrawing pcb = withLockedTrack();
            RoutingEngine engine = new RoutingEngine(board(pcb, Map.of()));
            assertTrue(engine.segmentCarried(pcb, 0, 0, 0, 2, WALKING, Optional.empty()).isEmpty());
            assertSame(pcb, engine.cornerCarried(pcb, 0, 1, point(10, 5), WALKING));
            assertEquals(pcb.tracks(), engine.cornersRounded(pcb, 2, List.of()).pcb().tracks());
            assertEquals(pcb.tracks(), engine.withoutJogs(pcb, List.of()).tracks());
        }

        @Test
        void aTrackOfALockedGroupIsHeldAsALockedOneIs() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 0, 20, 10)).arranged(Arrangement.in("front-panel"))))
                    .withGroups(List.of(new DrawnGroup("front-panel", DrawnState.LOCKED)));
            assertTrue(new RoutingEngine(board(pcb, Map.of())).segmentCarried(pcb, 0, 0, 0, 2, WALKING, Optional.empty()).isEmpty());
        }

    }
}


