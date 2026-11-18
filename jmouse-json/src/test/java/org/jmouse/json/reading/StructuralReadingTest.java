package org.jmouse.json.reading;

import org.jmouse.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.json.reading.JsonPatchChange.Operation.ADD;
import static org.jmouse.json.reading.JsonPatchChange.Operation.MOVE;
import static org.jmouse.json.reading.JsonPatchChange.Operation.REMOVE;
import static org.jmouse.json.reading.JsonPatchChange.Operation.REPLACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A patched document read again: what a change reached read anew, everything else the very value it was. */
class StructuralReadingTest {

    record Point(double x, double y) {
    }

    record Line(String layer, List<Point> points) {
    }

    record Board(String name, List<Line> lines, Map<String, Point> marks) {
    }

    private static final String BEFORE = """
            {"name": "video", "lines": [{"layer": "top", "points": [{"x": 0, "y": 0}, {"x": 1, "y": 0}]},
                                        {"layer": "bottom", "points": [{"x": 5, "y": 5}, {"x": 6, "y": 5}]}],
             "marks": {"origin": {"x": 0, "y": 0}, "corner": {"x": 9, "y": 9}}}""";

    private final StructuralReading<Object> reading = new StructuralReading<>(new JsonTreeAccess(Json.mapper()));

    private Board read(String text, Board before, ChangedPaths changed) {
        return (Board) reading.read(before, Json.read(text), Board.class, changed);
    }

    @Test
    void readsAgainOnlyTheElementAChangeReached() {
        Board before = Json.read(BEFORE, Board.class);
        String after = BEFORE.replace("{\"x\": 6, \"y\": 5}", "{\"x\": 6, \"y\": 4}");
        Board now = read(after, before, ChangedPaths.of(List.of(JsonPatchChange.at(REPLACE, "/lines/1/points/1/y"))));

        assertEquals(Json.read(after, Board.class), now);
        assertSame(before.lines().get(0), now.lines().get(0));
        assertSame(before.lines().get(1).points().get(0), now.lines().get(1).points().get(0));
        assertSame(before.marks(), now.marks());
        assertNotSame(before.lines().get(1), now.lines().get(1));
    }

    @Test
    void readsAListWholeWhereAnElementIsAddedOrTakenAway() {
        Board before = Json.read(BEFORE, Board.class);
        String after = BEFORE.replace("{\"x\": 1, \"y\": 0}]", "{\"x\": 1, \"y\": 0}, {\"x\": 2, \"y\": 0}]");
        Board now = read(after, before, ChangedPaths.of(List.of(JsonPatchChange.at(ADD, "/lines/0/points/2"))));

        assertEquals(Json.read(after, Board.class), now);
        assertSame(before.lines().get(1), now.lines().get(1));
    }

    @Test
    void readsAMapValueAChangeReachedAndKeepsTheOthers() {
        Board before = Json.read(BEFORE, Board.class);
        String after = BEFORE.replace("\"corner\": {\"x\": 9, \"y\": 9}", "\"corner\": {\"x\": 8, \"y\": 9}");
        Board now = read(after, before, ChangedPaths.of(List.of(JsonPatchChange.at(REPLACE, "/marks/corner/x"))));

        assertEquals(Json.read(after, Board.class), now);
        assertSame(before.marks().get("origin"), now.marks().get("origin"));
        assertSame(before.lines(), now.lines());
    }

    @Test
    void marksTheHoldersOfWhatIsAddedRemovedOrMoved() {
        ChangedPaths changed = ChangedPaths.of(List.of(JsonPatchChange.at(REMOVE, "/lines/0"),
                new JsonPatchChange(MOVE, "/marks/a~1b", Optional.of("/name"))));
        assertTrue(changed.at("lines").isWhole());
        assertTrue(changed.at("marks").isWhole());
        assertTrue(changed.isWhole());
    }

    @Test
    void keepsWhatWasReadWhereNothingChanged() {
        Board before = Json.read(BEFORE, Board.class);
        assertSame(before, read(BEFORE, before, ChangedPaths.none()));
    }
}
