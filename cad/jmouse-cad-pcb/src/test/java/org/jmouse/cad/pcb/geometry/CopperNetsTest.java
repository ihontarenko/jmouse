package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Which net a via is on: the pads its copper reaches, else the pour it stands in. */
class CopperNetsTest {

    private static final String SQUARE = "M 0 0 L 20 0 L 20 20 L 0 20 Z";

    /** One surface pad of net {@code SIGNAL} at 15, 15 on the top, a pour of {@code GROUND} on both sides, and the copper given. */
    private static CopperNets netsOf(List<Track> tracks, List<Via> vias) {
        FootprintDrawing footprint = new FootprintDrawing(List.of(new FootprintDrawing.Pad("1", "surface", "rectangle", 0, 0, 0, 1, 1,
                0, 0, 0, "", "", "", null, null)), List.of(), List.of(), null, List.of());
        Placement part = new Placement("TP1", 15, 15, 0, PcbVocabulary.TOP, Optional.empty());
        List<Zone> pours = List.of(new Zone(PcbVocabulary.TOP, "GROUND", 0, 0, SQUARE),
                new Zone(PcbVocabulary.BOTTOM, "GROUND", 0, 0, SQUARE));
        PcbDrawing pcb = new PcbDrawing("circuit", Optional.empty(), List.of(), null, List.of(), List.of(), 2, SQUARE, List.of(part),
                tracks, List.of(), vias, pours, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        return CopperNets.of(pcb, Map.of("TP1", footprint), Map.of("TP1", Map.of("1", "SIGNAL")), new PadOutline(16, 6), 16);
    }

    @Test
    void aViaNothingButAPourReachesIsThePours() {
        CopperNets nets = netsOf(List.of(), List.of(new Via(5, 5, 0.3, 0.6, null, null)));
        assertEquals(Optional.of("GROUND"), nets.viaNets().getFirst());
    }

    @Test
    void aViaATrackJoinsToAPadKeepsThePadsNetInsideAPour() {
        Track track = new Track(PcbVocabulary.TOP, 0.25, 0, List.of(new Point(5, 5), new Point(15, 15)));
        CopperNets nets = netsOf(List.of(track), List.of(new Via(5, 5, 0.3, 0.6, null, null)));
        assertEquals(Optional.of("SIGNAL"), nets.viaNets().getFirst());
        assertEquals(Optional.of("SIGNAL"), nets.trackNets().getFirst());
    }

    /**
     * A line cut back from a pad that moved — its tip left near the pad's new place — is where the connection still to lay begins,
     * not the far pad it runs from: on 2026-11-15 the autorouter laid a new track all the way to the far pad beside such a tip.
     */
    @Test
    void aConnectionStillToLayRunsFromTheNearestTipOfALineNotItsFarPad() {
        FootprintDrawing footprint = new FootprintDrawing(List.of(new FootprintDrawing.Pad("1", "surface", "rectangle", 0, 0, 0, 1, 1,
                0, 0, 0, "", "", "", null, null)), List.of(), List.of(), null, List.of());
        Placement far = new Placement("A", 0, 0, 0, PcbVocabulary.TOP, Optional.empty());
        Placement moved = new Placement("B", 12, 2, 0, PcbVocabulary.TOP, Optional.empty());
        Track cutBack = new Track(PcbVocabulary.TOP, 0.25, 0, List.of(new Point(0, 0), new Point(10, 0)));
        PcbDrawing pcb = new PcbDrawing("circuit", Optional.empty(), List.of(), null, List.of(), List.of(), 2, "M -5 -5 L 20 -5 L 20 20 L -5 20 Z",
                List.of(far, moved), List.of(cutBack), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of());
        CopperNets nets = CopperNets.of(pcb, Map.of("A", footprint, "B", footprint), Map.of("A", Map.of("1", "N"), "B", Map.of("1", "N")),
                new PadOutline(16, 6), 16);
        CopperNets.Unlaid unlaid = nets.unlaid().getFirst();
        assertEquals(List.of(new Point(10, 0), new Point(12, 2)), List.of(unlaid.from(), unlaid.to()));
    }

    @Test
    void aViaOutsideEveryPourAndPadIsOnNoNet() {
        CopperNets nets = netsOf(List.of(), List.of(new Via(30, 30, 0.3, 0.6, null, null)));
        assertEquals(Optional.empty(), nets.viaNets().getFirst());
    }
}
