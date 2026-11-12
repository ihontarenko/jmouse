package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
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
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final PcbChecks.Settings CHECKS = new PcbChecks.Settings(0.005, 6, CURVE_STEPS,
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

    private static PcbDrawing.Track track(List<Point> points) {
        return track(points, PcbDrawing.TOP, 0.25);
    }

    private static PcbDrawing.Track track(List<Point> points, String layer, double width) {
        return new PcbDrawing.Track(layer, width, 0, points);
    }

    private static PcbDrawing.Placement place(String designator, double x, double y) {
        return new PcbDrawing.Placement(designator, x, y, 0, PcbDrawing.TOP, Optional.empty());
    }

    private static PcbDrawing.Via via(double x, double y, double diameter) {
        return new PcbDrawing.Via(x, y, 0.3, diameter, PcbDrawing.TOP, PcbDrawing.BOTTOM);
    }

    private static PcbDrawing pcb(List<PcbDrawing.Placement> placements, List<PcbDrawing.Track> tracks, List<PcbDrawing.Via> vias,
                                  List<PcbDrawing.Zone> zones, List<PcbDrawing.NetClass> netClasses) {
        return new PcbDrawing("circuit", Optional.empty(), netClasses, null, List.of(), List.of(), 2, OUTLINE, placements, tracks, List.of(), vias,
                zones, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static PcbDrawing pcb(List<PcbDrawing.Placement> placements, List<PcbDrawing.Track> tracks) {
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
            named.put(placement.designator(), Optional.of("FP.TEST:PART"));
        });
        return new RoutingBoard(footprints, named, parts, VALUES, Map.of(LINK, CHIP), CHECKS, new PadOutline(CURVE_STEPS, 6), SETTINGS);
    }

    private static RoutingBoard board(PcbDrawing pcb, Map<String, Map<String, String>> parts) {
        return board(pcb, parts, CHIP);
    }

    private static TrackRouter.LayingStyle style(TrackRouter.RoutingMode mode, double angleStep, Tightening.JogHandling jogs) {
        return new TrackRouter.LayingStyle(mode, angleStep, jogs);
    }

    private static final TrackRouter.LayingStyle WALKING = style(TrackRouter.RoutingMode.WALKAROUND, 45, Tightening.JogHandling.KEPT);

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
                double inward = ((before.x() - was.x()) * dx + (before.y() - was.y()) * dy) / (Lines.lengthBetween(was, before) * length);
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
        PcbDrawing.Track line = pcb.tracks().get(index);
        RoutingWorld world = new RoutingWorld(pcb, board);
        TrackFence fence = world.fence(line.layer(), world.netOfTrack(index), line.width(), OptionalInt.of(index), true);
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
            assertTrue(Lines.lengthBetween(expected.get(index), actual.get(index)) < 1e-6, "expected " + expected + " but was " + actual);
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

    @Nested
    class APartCarriedItsTrackFollowing {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");

        private Point padOf(PcbDrawing.Placement placement, int side) {
            return point(placement.x() + side * 1.5, placement.y());
        }

        /** The part B moved by an offset, as dragging leaves the pcb: the track's end carried with it, the rest of it as it was. */
        private PcbDrawing moved(PcbDrawing pcb, double dx, double dy) {
            List<PcbDrawing.Placement> placements = pcb.placements().stream().map(placement -> placement.designator().equals("B")
                    ? new PcbDrawing.Placement("B", placement.x() + dx, placement.y() + dy, 0, PcbDrawing.TOP, Optional.empty()) : placement).toList();
            List<PcbDrawing.Track> tracks = pcb.tracks().stream().map(track -> {
                List<Point> points = new ArrayList<>(track.points());
                Point last = points.getLast();
                points.set(points.size() - 1, point(last.x() + dx, last.y() + dy));
                return track.withPoints(points);
            }).toList();
            return pcb.withPlacements(placements).withTracks(tracks);
        }

        private boolean onThePartBeside(double dx, double dy) {
            return Math.abs(12 + dx) < 5 && Math.abs(dy) < 3;
        }

        @Test
        void keepsAShapelyLineWhereverItGoesInFreeSpace() {
            PcbDrawing.Placement a = place("A", 0, 0);
            PcbDrawing.Placement b = place("B", 12, 0);
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
                        PcbDrawing laid = engine.tracksLaid(pcb, moved(pcb, dx, dy), 45);
                        List<Point> line = laid.tracks().getFirst().points();
                        assertClean(faultsOfTrack(board, laid, 0), name + " by " + dx + ", " + dy);
                        // In free space there is nothing to go round: the way is about as short as it can be.
                        assertTrue(Lines.lengthOf(line) <= across(line.getFirst(), line.getLast()) + 0.01, name + " by " + dx + ", " + dy + ": " + line);
                    }
                }
            });
        }

        @Test
        void goesRoundWhatStandsInTheWay() {
            PcbDrawing.Placement a = place("A", 0, 0);
            PcbDrawing.Placement b = place("B", 12, 0);
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
                    assertClean(faultsOfTrack(board, engine.tracksLaid(pcb, moved(pcb, dx, dy), 45), 0), "by " + dx + ", " + dy);
                }
            }
        }

        @Test
        void answersTheSameAFrameAtATimeAsWhenItIsLetGoInFreeSpace() {
            PcbDrawing.Placement a = place("A", 0, 0);
            PcbDrawing.Placement b = place("B", 12, 0);
            PcbDrawing pcb = pcb(List.of(a, b), List.of(track(List.of(padOf(a, 1), padOf(b, -1)))));
            RoutingBoard board = board(pcb, parts);
            RoutingEngine engine = new RoutingEngine(board);
            for (double[] way : RING) {
                assertClean(faultsOfTrack(board, engine.tracksFollowing(pcb, moved(pcb, way[0] * 4, way[1] * 4), 45), 0), "towards " + way[0] + ", " + way[1]);
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
                        List<Point> carriedLine = new ArrayList<>(points);
                        carriedLine.set(carriedLine.size() - 1, point(points.getLast().x() + dx, points.getLast().y() + dy));
                        PcbDrawing carried = pcb.withPlacements(List.of(place("A", 0, 0), place("B", 12 + dx, standing + dy)))
                                .withTracks(List.of(track(carriedLine)));
                        List<Point> now = engine.tracksFollowing(pcb, carried, 45).tracks().getFirst().points();
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
        private final List<PcbDrawing.Placement> placements = List.of(place("A", 0, 10), place("B", 20, 10), place("C", -5, 6.5), place("D", 25, 6.5));
        private final List<Point> under = line(1.5, 10, 4, 7.5, 16, 7.5, 18.5, 10);
        private final List<Point> over = line(-3.5, 6.5, 23.5, 6.5);

        private void neverComesNearerThanItsGap(TrackRouter.RoutingMode mode) {
            PcbDrawing pcb = pcb(placements, List.of(track(under), track(over)));
            RoutingBoard board = board(pcb, parts);
            RoutingEngine engine = new RoutingEngine(board);
            double asked = Lines.lengthOf(under);
            for (double pull = 0.1; pull <= 4; pull += 0.1) {
                Optional<PcbDrawing> answer = engine.segmentCarried(pcb, 0, 1, 0, -pull, style(mode, 45, Tightening.JogHandling.MERGED), Optional.empty());
                if (answer.isEmpty()) {
                    continue;
                }
                for (int index : new int[] {0, 1}) {
                    assertClean(faultsOfTrack(board, answer.get(), index), "track " + index + ", pulled " + pull);
                }
                List<Point> line = answer.get().tracks().getFirst().points();
                assertTrue(Lines.lengthOf(line) <= asked * SETTINGS.carrying().longestWalk() + 2 * pull, "pulled " + pull + ": " + line);
            }
        }

        @Test
        void walkingNeverComesNearerThanItsGapNorGoesRoundTheFarEndOfIt() {
            neverComesNearerThanItsGap(TrackRouter.RoutingMode.WALKAROUND);
        }

        @Test
        void shovingNeverComesNearerThanItsGapNorGoesRoundTheFarEndOfIt() {
            neverComesNearerThanItsGap(TrackRouter.RoutingMode.SHOVE);
        }
    }

    @Nested
    class ALineMadeOfSeveralTracksAsABoardReadFromAFileHasIt {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");
        private final List<PcbDrawing.Placement> placements = List.of(place("A", 0, 0), place("B", 21.5, 15));
        private final List<PcbDrawing.Track> tracks = List.of(track(line(1.5, 0, 1.5, 10), PcbDrawing.TOP, 0.5),
                track(line(1.5, 10, 6.5, 15), PcbDrawing.TOP, 0.4), track(line(20, 15, 6.5, 15), PcbDrawing.TOP, 0.3));

        @Test
        void movesTheDiagonalParallelTheTracksEitherSideGrowingAndShrinkingEachKeepingItsOwnWidth() {
            PcbDrawing pcb = pcb(placements, tracks);
            PcbDrawing answer = new RoutingEngine(board(pcb, parts)).segmentCarried(pcb, 1, 0, -2, 2, WALKING, Optional.empty()).orElseThrow();
            assertLine(line(1.5, 0, 1.5, 14), answer.tracks().get(0).points());
            assertLine(line(1.5, 14, 2.5, 15), answer.tracks().get(1).points());
            assertLine(line(20, 15, 2.5, 15), answer.tracks().get(2).points());
            assertEquals(List.of(0.5, 0.4, 0.3), answer.tracks().stream().map(PcbDrawing.Track::width).toList());
        }

        @Test
        void movesItTheOtherWayAsWellAndAsFarAsItGoesWithoutAStep() {
            PcbDrawing pcb = pcb(placements, tracks);
            RoutingEngine engine = new RoutingEngine(board(pcb, parts));
            for (double distance : new double[] {0.5, 1, 1.5, 2, 3}) {
                PcbDrawing answer = engine.segmentCarried(pcb, 1, 0, distance, -distance, WALKING, Optional.empty()).orElseThrow();
                List<Point> third = Lines.reversed(answer.tracks().get(2).points());
                List<Point> whole = Lines.joined(answer.tracks().get(0).points(), answer.tracks().get(1).points().subList(1, answer.tracks().get(1).points().size()),
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
                    List.of(track(line(83.966, 44.819, 83.966, 27.171), PcbDrawing.TOP, 2.5), track(line(88.875, 49.816, 83.922, 44.863), PcbDrawing.TOP, 2.5)));
            RoutingEngine engine = new RoutingEngine(board(pcb, nets("A", "1", "N", "A", "2", "N", "B", "1", "N", "B", "2", "N")));
            for (double distance : new double[] {0.5, 1, 2, 3}) {
                PcbDrawing answer = engine.segmentCarried(pcb, 1, 0, -distance, distance, WALKING, Optional.empty()).orElseThrow();
                List<Point> vertical = answer.tracks().get(0).points();
                List<Point> diagonal = answer.tracks().get(1).points();
                assertLine(List.of(diagonal.getLast()), List.of(vertical.getFirst()));
                assertEquals(83.966, vertical.getFirst().x(), 1e-3, "by " + distance + ": the vertical stays vertical");
                assertTrue(vertical.getFirst().y() > 44.9, "by " + distance + ": it grows down");
                assertTrue(diagonal.size() <= 4, "by " + distance + ": " + diagonal);
                List<Point> back = Lines.reversed(diagonal);
                assertClean(faultsOfLine(Lines.joined(Lines.reversed(vertical), back.subList(1, back.size()))), "by " + distance);
            }
        }

        @Test
        void isNotJoinedAcrossAPadOrAVia() {
            PcbDrawing pcb = pcb(placements, tracks, List.of(new PcbDrawing.Via(1.5, 10, 0.3, 0.6, null, null)), List.of(), List.of());
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
            PcbDrawing pcb = pcb(List.of(), List.of(track(jogged, PcbDrawing.TOP, 0.2)));
            assertLine(jogged, new RoutingEngine(board(pcb, Map.of())).withoutJogs(pcb, List.of()).tracks().getFirst().points());
        }

        @Test
        void isMergedByADragOnlyWhenTheReaderAsksForIt() {
            // A jog one can see — 0.7 mm on a 1 mm track: longer than a step nobody means, shorter than a run.
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 10, 10.5, 9.5, 30, 9.5, 40.5, 20, 50, 20), PcbDrawing.TOP, 1)));
            RoutingEngine engine = new RoutingEngine(board(pcb, Map.of()));
            PcbDrawing kept = engine.segmentCarried(pcb, 0, 3, 0, 1, WALKING, Optional.empty()).orElseThrow();
            PcbDrawing merged = engine.segmentCarried(pcb, 0, 3, 0, 1, style(TrackRouter.RoutingMode.WALKAROUND, 45, Tightening.JogHandling.MERGED),
                    Optional.empty()).orElseThrow();
            assertLine(line(0, 0, 10, 10, 10.5, 9.5), kept.tracks().getFirst().points().subList(0, 3));
            assertEquals(kept.tracks().getFirst().points().size() - 1, merged.tracks().getFirst().points().size());
            assertClean(faultsOfLine(merged.tracks().getFirst().points()), "merged");
        }

        @Test
        void isMergedOnAFinishedBoardOnlyOnTheTracksAskedAndNeverIntoAnotherNetsCopper() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(jogged, PcbDrawing.TOP, 1), track(line(30, 0, 40, 10, 40.3, 9.7, 50, 9.7), PcbDrawing.TOP, 1)));
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
        void doesNotStopShortOfWhatIsInItsWayTheStretchGoesAsFarAcrossAsItIsPulledAndRoundIt() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 0, 5, 20, 25, 20, 30))), List.of(via(5, 18, 2)), List.of(), List.of());
            RoutingBoard board = board(pcb, Map.of());
            // Pulled 8 to the left, the diagonal goes 4√2 across itself: onto y = x + 13, where the via stands.
            PcbDrawing answer = new RoutingEngine(board).segmentCarried(pcb, 0, 1, -8, 0, WALKING, Optional.of(point(16, 21))).orElseThrow();
            assertClean(faultsOfTrack(board, answer, 0), "round the via");
            List<Point> line = answer.tracks().getFirst().points();
            boolean reached = false;
            for (int index = 1; index < line.size(); index++) {
                Point point = line.get(index);
                Point was = line.get(index - 1);
                boolean diagonal = Math.abs((point.y() - point.x()) - (was.y() - was.x())) < 1e-6 && point.x() != was.x();
                reached |= diagonal && point.y() - point.x() >= 13 - 1e-3;
            }
            assertTrue(reached, "" + line);
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
        void shovesANeighbourClearAndBothStayShapely() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 3, 0, 17, 0, 20, 0)), track(line(-30, 1, 50, 1))));
            RoutingBoard board = board(pcb, Map.of());
            RoutingEngine engine = new RoutingEngine(board);
            for (double distance : new double[] {0.8, 1.2, 2}) {
                PcbDrawing answer = engine.segmentCarried(pcb, 0, 1, 0, distance, style(TrackRouter.RoutingMode.SHOVE, 45, Tightening.JogHandling.KEPT),
                        Optional.empty()).orElseThrow();
                assertClean(faultsOfTrack(board, answer, 0), "by " + distance);
                assertClean(faultsOfTrack(board, answer, 1), "the other, by " + distance);
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
        private final List<PcbDrawing.Placement> placements = List.of(place("A", 0, 0), place("B", 15, 0), place("C", 30, 0), place("D", 30, 15),
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
            for (int drag = 0; drag < 40; drag++) {
                int chosen = (int) Math.floor(random.next() * now.placements().size());
                PcbDrawing.Placement part = now.placements().get(chosen);
                double dx = (random.next() - 0.5) * 6;
                double dy = (random.next() - 0.5) * 6;
                List<Point> ends = CHIP.pads().stream().map(each -> point(part.x() + each.x(), part.y() + each.y())).toList();
                UnaryOperator<Point> carriedPoint = point -> point(point.x() + dx, point.y() + dy);
                java.util.function.Predicate<Point> moved = point -> ends.stream().anyMatch(end -> Lines.lengthBetween(end, point) < 1e-3);
                List<PcbDrawing.Placement> placed = new ArrayList<>(now.placements());
                placed.set(chosen, new PcbDrawing.Placement(part.designator(), part.x() + dx, part.y() + dy, 0, PcbDrawing.TOP, Optional.empty()));
                List<PcbDrawing.Track> tracks = now.tracks().stream().map(track -> {
                    List<Point> points = track.points();
                    // A track between two pads of the part goes along whole, as a selection moving does.
                    if (moved.test(points.getFirst()) && moved.test(points.getLast())) {
                        return track.withPoints(points.stream().map(carriedPoint).toList());
                    }
                    List<Point> changed = new ArrayList<>(points);
                    boolean any = false;
                    for (int index : new int[] {0, points.size() - 1}) {
                        if (moved.test(points.get(index))) {
                            changed.set(index, carriedPoint.apply(points.get(index)));
                            any = true;
                        }
                    }
                    return any ? track.withPoints(changed) : track;
                }).toList();
                now = engine.tracksLaid(now, now.withPlacements(placed).withTracks(tracks), 45);
                for (int index = 0; index < now.tracks().size(); index++) {
                    assertClean(faultsOfLine(now.tracks().get(index).points(), false), "drag " + drag + ", track " + index);
                    if (!faultsOfTrack(board, now, index, false).isEmpty()) {
                        notClear += 1;
                    }
                }
            }
            // Where parts are pushed onto each other's copper some must stand too near, and the checks say so; it must stay the exception.
            assertTrue(notClear < 60, "tracks too near other copper over all the drags: " + notClear);
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
                    style(TrackRouter.RoutingMode.WALKAROUND, 45, Tightening.JogHandling.MERGED), Optional.empty()).orElseThrow();
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
        TrackRouter.RoutedLeg round = new RoutingEngine(board).leg(pcb, PcbDrawing.TOP, Optional.of("A"), 0.25, point(5, 0), point(5, 10), 45,
                TrackRouter.RoutingMode.WALKAROUND, 0).leg();
        assertTrue(round.reached());
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        tracks.add(track(Lines.joined(List.of(point(5, 0)), round.points())));
        assertClean(faultsOfTrack(board, pcb.withTracks(tracks), 1), "the leg laid");
        PcbDrawing walled = pcb(List.of(), List.of(track(line(-100, 5, 100, 5))));
        assertFalse(new RoutingEngine(board(walled, Map.of())).leg(walled, PcbDrawing.TOP, Optional.of("A"), 0.25, point(5, 0), point(5, 10), 45,
                TrackRouter.RoutingMode.WALKAROUND, 0).leg().reached());
    }

    /**
     * A leg is found with sharp corners, and drawn with the reader's: a corner rounded inward comes nearer what stands inside it.
     * On lm317-supply a leg turned down past RV1's pad 1 with 0.45 mm to spare as found and 0.04 mm as drawn with 1 mm corners —
     * the checks said so on 2026-11-12. The corners it is answered with are those that keep its gaps.
     */
    @Test
    void aTrackBeingLaidIsDrawnOnlyWithCornersThatKeepItsGaps() {
        // A via inside the corner a leg turns at, clear of both its runs and in the way of a corner rounded by 4 mm.
        PcbDrawing pcb = pcb(List.of(), List.of(), List.of(new PcbDrawing.Via(8.8, 1.2, 0.2, 0.4, PcbDrawing.TOP, PcbDrawing.BOTTOM)), List.of(), List.of());
        RoutingBoard board = board(pcb, Map.of());
        RoutingEngine.LaidLeg laid = new RoutingEngine(board).leg(pcb, PcbDrawing.TOP, Optional.of("A"), 0.25, point(0, 0), point(10, 10), 90,
                TrackRouter.RoutingMode.WALKAROUND, 4);
        assertLine(line(10, 0, 10, 10), laid.leg().points());
        assertTrue(laid.leg().reached());
        assertTrue(laid.rounding() < 4, "corners of 4 mm would run over the via: " + laid.rounding());
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        tracks.add(new PcbDrawing.Track(PcbDrawing.TOP, 0.25, laid.rounding(), Lines.joined(List.of(point(0, 0)), laid.leg().points())));
        RoutingWorld world = new RoutingWorld(pcb.withTracks(tracks), board);
        assertTrue(world.fence(PcbDrawing.TOP, Optional.of("A"), 0.25, OptionalInt.of(0), true).copperClear(
                CopperShapes.track(tracks.getLast(), CURVE_STEPS)), "drawn as answered, it keeps its gaps");
    }

    @Nested
    class TheReadersSettings {

        private final Map<String, Map<String, String>> parts = nets("A", "2", "N", "B", "1", "N");

        private List<Double> angles(PcbDrawing.Track line) {
            List<Double> angles = new ArrayList<>();
            for (int index = 1; index < line.points().size(); index++) {
                Point from = line.points().get(index - 1);
                Point to = line.points().get(index);
                angles.add(Math.toDegrees(Math.atan2(to.y() - from.y(), to.x() - from.x())));
            }
            return angles;
        }

        private Autorouter.Routed route(double angleStep, double rounding, List<PcbDrawing.Track> tracks, List<PcbDrawing.NetClass> netClasses) {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 12, 7)), tracks, List.of(), List.of(), netClasses);
            return autorouted(board(pcb, parts), pcb, angleStep, rounding, Autorouter.Crossing.VIAS, moment -> {
            });
        }

        @Test
        void laysAtRightAnglesWhenAsked() {
            Autorouter.Routed answer = route(90, 0, List.of(), List.of());
            assertEquals(List.of(), answer.unrouted());
            for (PcbDrawing.Track line : answer.pcb().tracks()) {
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
            List<PcbDrawing.NetClass> classes = List.of(new PcbDrawing.NetClass("power", 0.5, 0.4, 0, 0, List.of("N", "W")));
            Autorouter.Routed answer = route(45, 0, List.of(track(line(-100, 3.2, 3, 3.2, 3, 100))), classes);
            List<PcbDrawing.Track> laid = answer.pcb().tracks().subList(1, answer.pcb().tracks().size());
            assertFalse(laid.isEmpty());
            assertTrue(laid.stream().allMatch(line -> line.width() == 0.5));
            RoutingWorld world = new RoutingWorld(answer.pcb(), board(answer.pcb(), parts));
            TrackFence fence = world.fence(PcbDrawing.TOP, Optional.of("N"), 0.5);
            for (PcbDrawing.Track line : laid) {
                assertTrue(fence.lineClear(line.points()), "" + line.points());
            }
            assertEquals(0.4, world.clearanceOf(Optional.of("N")));
        }

        @Test
        void carriesASegmentAtRightAnglesAsTracksOfRightAnglesWant() {
            PcbDrawing pcb = pcb(List.of(), List.of(track(line(0, 0, 10, 0, 10, 10, 20, 10))));
            PcbDrawing answer = new RoutingEngine(board(pcb, Map.of())).segmentCarried(pcb, 0, 0, 0, 3,
                    style(TrackRouter.RoutingMode.WALKAROUND, 90, Tightening.JogHandling.KEPT), Optional.empty()).orElseThrow();
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
            assertTrue(answer.pcb().tracks().stream().allMatch(line -> line.layer().equals(PcbDrawing.TOP)));
            assertEquals(List.of(), answer.pcb().vias());
        }

        @Test
        void goesThroughViasUnderAWallComingBackToThePadsSide() {
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 0, 12)), List.of(track(line(-200, 6, 200, 6))));
            Autorouter.Routed answer = autorouted(board(pcb, parts), pcb);
            assertEquals(List.of(), answer.unrouted());
            assertEquals(2, answer.pcb().vias().size());
            List<PcbDrawing.Track> laid = answer.pcb().tracks().subList(1, answer.pcb().tracks().size());
            assertTrue(laid.stream().anyMatch(line -> line.layer().equals(PcbDrawing.BOTTOM)));
            for (Point end : line(1.5, 0, 1.5, 12, -1.5, 12)) {
                assertTrue(laid.stream().filter(line -> line.points().stream().anyMatch(point -> Lines.lengthBetween(point, end) < 0.001))
                        .allMatch(line -> line.layer().equals(PcbDrawing.TOP)));
            }
        }

        @Test
        void crossesByALinkWhereNothingElseGoesAndOnlyWhenAllowed() {
            // A wall on both layers: no via gets past it, only a part standing over the top one.
            PcbDrawing pcb = pcb(List.of(place("A", 0, 0), place("B", 0, 12)), List.of(track(line(-300, 6, 300, 6)),
                    track(line(-300, 6, 300, 6), PcbDrawing.BOTTOM, 0.25)));
            RoutingBoard board = board(pcb, parts);
            Autorouter.Routed refused = autorouted(board, pcb, 45, 0, Autorouter.Crossing.VIAS, moment -> {
            });
            assertEquals(1, refused.unrouted().size());
            Autorouter.Routed linked = autorouted(board, pcb, 45, 0, Autorouter.Crossing.LINKS, moment -> {
            });
            List<PcbDrawing.Placement> links = linked.pcb().placements().stream().filter(placement -> placement.footprint().isPresent()).toList();
            assertEquals(List.of(), linked.unrouted());
            assertEquals(1, links.size());
            assertEquals(Map.of("1", "N", "2", "N"), links.getFirst().nets());
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

        private final PcbDrawing.Zone ground = new PcbDrawing.Zone(PcbDrawing.BOTTOM, "0", 0.4, 0, "M-20 -20H20V20H-20Z");

        @Test
        void takesTheSurfacePadsOfItsNetOnTheOtherSideEachByADropOfItsOwn() {
            PcbDrawing pcb = pcb(List.of(place("A", -8, 0), place("B", 8, 0)), List.of(), List.of(), List.of(ground), List.of());
            Autorouter.Routed answer = autorouted(board(pcb, nets("A", "1", "0", "B", "2", "0")), pcb);
            assertEquals(List.of(), answer.unrouted());
            assertEquals(2, answer.pcb().vias().size());
            assertEquals(2, answer.pcb().tracks().size());
            for (PcbDrawing.Track drop : answer.pcb().tracks()) {
                assertTrue(Lines.lengthOf(drop.points()) < SETTINGS.autorouting().planeDrops().reach());
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
                assertTrue(Lines.lengthOf(line) > Lines.lengthOf(pcb.tracks().getFirst().points()) + 1);
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
                    assertTrue(Lines.lengthBetween(line.get(index - 1), line.get(index)) >= 0.25 * SETTINGS.routing().leastStep(), "" + line);
                }
            }
        }
    }

    @Test
    void aStepNobodyCanMeanGoesFromALineWhateverBecomesOfJogs() {
        PcbDrawing pcb = pcb(List.of(), List.of());
        TrackFence fence = new RoutingWorld(pcb, board(pcb, Map.of())).fence(PcbDrawing.TOP, Optional.empty(), 0.25);
        Tightening tightening = new Tightening(fence, 45, List.of(), SETTINGS.routing());
        assertLine(line(52.5, 22.78, 60.85, 22.78, 60.88, 22.75),
                tightening.untangled(line(52.5, 22.78, 59.85, 22.78, 59.88, 22.75, 60.88, 22.75), Tightening.JogHandling.KEPT));
        // A jog one can see is the reader's, kept.
        List<Point> seen = line(0, 0, 10, 0, 10.5, 0.5, 20, 0.5);
        assertLine(seen, tightening.untangled(seen, Tightening.JogHandling.KEPT));
    }
}
