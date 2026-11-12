package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.kicad.OwnBodies;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A board's footprint stands with our body only where our land pattern is the same, the body shifted as the pads are. */
class OwnBodiesTest {

    private static final FootprintDrawing OURS = FootprintDrawing.of(CadLibrary.library()
            .findCoded(KiCadLibraryMap.standard().footprintCodeOf("Capacitor_THT:CP_Radial_D10.0mm_P5.00mm").orElseThrow()).orElseThrow());

    /** Our pads as a board would carry them, moved by (dx, dy) in the pads' own frame, Y down. */
    private static List<FootprintLines.Pad> board(double dx, double dy) {
        return OURS.pads().stream().map(pad -> new FootprintLines.Pad(pad.number(), pad.type(), pad.shape(), pad.x() + dx, pad.y() + dy, pad.rotation(),
                pad.width(), pad.height(), pad.drillWidth(), pad.drillHeight(), pad.roundness(), Optional.empty(), pad.face(), pad.opens(), null, null)).toList();
    }

    private static Optional<List<FootprintLines.Model>> bodies(List<FootprintLines.Pad> pads) {
        return OwnBodies.of(pads, OURS);
    }

    @Test
    void theSameLandPatternShiftedTakesOurBodyShiftedAlike() {
        List<FootprintLines.Model> models = bodies(board(-2.5, 1)).orElseThrow();
        assertEquals(OURS.models().getFirst().reference(), models.getFirst().reference());
        // Pads run down, the model's Y up: a pad 1 lower is the body 1 lower.
        assertEquals(OURS.models().getFirst().offset().get(0) - 2.5, models.getFirst().offset().x(), 1e-9);
        assertEquals(OURS.models().getFirst().offset().get(1) - 1, models.getFirst().offset().y(), 1e-9);
    }

    @Test
    void anotherLandPatternKeepsWhatItNames() {
        List<FootprintLines.Pad> wider = board(0, 0).stream().map(pad -> pad.number().equals("2")
                ? new FootprintLines.Pad(pad.number(), pad.type(), pad.shape(), pad.x() + 2.5, pad.y(), pad.rotation(), pad.width(), pad.height(),
                pad.drillWidth(), pad.drillHeight(), pad.roundness(), Optional.empty(), pad.face(), pad.opens(), null, null) : pad).toList();
        assertTrue(bodies(wider).isEmpty());
        assertTrue(bodies(board(0, 0).subList(0, 1)).isEmpty(), "a pad fewer is another footprint");
    }
}
