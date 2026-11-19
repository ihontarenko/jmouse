package org.jmouse.cad.library;

import org.jmouse.cad.pcb.PcbWords;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.el.grammar.ClauseLineNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A track's corners said in a pcb's text — the radius asked of them, and each corner's own where it keeps a smaller one —, read and
 * written back the same.
 */
class TrackCornersTest {

    private static PcbDrawing pcb(String body) {
        return PcbDrawing.of(CadLanguage.shipped().language().readOne("""
                pcb:corners-board("Corners board") {
                    circuit(nowhere)
                    layers(2)
                %s
                }
                """.formatted(body.indent(4)), "corners-board"));
    }

    private static ClauseLineNode trackLine(PcbDrawing board) {
        return board.lines().stream().filter(line -> "track".equals(line.word())).findFirst().orElseThrow();
    }

    @Test
    void readsEachCornersOwnRadius() {
        Track track = pcb("track(top, width: 0.25, rounding: 3, corners: [3, 0.5], from: 0 0, to: 10 0, to: 10 10, to: 20 10)").tracks().getFirst();
        assertEquals(3, track.rounding());
        assertEquals(List.of(3.0, 0.5), track.corners());
        assertEquals(0.5, track.roundingAt(1));
    }

    @Test
    void aTrackEveryCornerOfWhichKeepsItsRoundingSaysNoCorners() {
        PcbDrawing board = pcb("track(top, width: 0.25, rounding: 3, from: 0 0, to: 10 0, to: 10 10)");
        assertEquals(List.of(), board.tracks().getFirst().corners());
        assertEquals(3, board.tracks().getFirst().roundingAt(0));
        assertFalse(trackLine(board).has(PcbWords.CORNERS));
    }

    @Test
    void writesEachCornersOwnRadiusBack() {
        PcbDrawing board = pcb("track(top, width: 0.25, rounding: 3, corners: [3, 0.5], from: 0 0, to: 10 0, to: 10 10, to: 20 10)");
        assertEquals(List.of(3.0, 0.5), trackLine(board).quantities(PcbWords.CORNERS));
    }

    @Test
    void refusesCornersThatAreNotOneACorner() {
        assertThrows(RuntimeException.class,
                () -> pcb("track(top, width: 0.25, rounding: 3, corners: [3], from: 0 0, to: 10 0, to: 10 10, to: 20 10)").tracks().getFirst());
    }
}
