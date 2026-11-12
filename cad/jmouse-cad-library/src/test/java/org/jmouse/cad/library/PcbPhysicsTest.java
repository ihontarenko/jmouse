package org.jmouse.cad.library;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.physics.CopperPaths;
import org.jmouse.cad.pcb.physics.CopperPhysics;
import org.jmouse.cad.pcb.physics.StackPhysics;
import org.jmouse.cad.pcb.physics.TrackStacks;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine's physics of a pcb's copper and stack: the copper between two points in ohms, whatever way the copper was
 * drawn — a track, lines drawn as a shape, a filled area —, and a track's impedance and a pcb's warmth. Moved from the
 * editor's own tests with the physics on 2026-11-11: the editor asks the engine.
 */
class PcbPhysicsTest {

    private static final CopperPhysics COPPER = read(CopperPhysics.class, CopperPhysics.SECTION.resource(), CopperPhysics.class);
    private static final TrackStacks STACKS = new TrackStacks(read(StackPhysics.class, StackPhysics.SECTION.resource(), StackPhysics.class));

    private static final Map<String, Double> VALUES = Map.of("copper-thickness", 0.035, "inner-copper-thickness", 0.035,
            "pcb-thickness", 1.6, "plating-thickness", 0.025);

    private static final int CURVE_STEPS = 16;

    private static <T> T read(Class<?> owner, String resource, Class<T> type) {
        try (InputStream stream = owner.getResourceAsStream(resource)) {
            return JsonMapper.builder().build().readValue(stream, type);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static CopperPaths.Answer between(String body, double fromX, double fromY, double toX, double toY) {
        PcbDrawing pcb = PcbDrawing.of(CadLanguage.shipped().language().readOne("""
                pcb:physics-probe("Physics probe") {
                    layers(2)
                %s
                }
                """.formatted(body), "physics-probe"));
        CopperNets nets = CopperNets.of(pcb, Map.of(), Map.of(), new PadOutline(CURVE_STEPS, 6), CURVE_STEPS);
        return CopperPaths.between(pcb, nets, VALUES, new CopperPaths.Probe(fromX, fromY, Optional.of("top"), 0.1),
                new CopperPaths.Probe(toX, toY, Optional.of("top"), 0.1), 1, 10, COPPER, CURVE_STEPS);
    }

    private static CopperPaths.Path path(String body, double fromX, double fromY, double toX, double toY) {
        return assertInstanceOf(CopperPaths.Found.class, between(body, fromX, fromY, toX, toY)).path();
    }

    @Test
    void aTracksResistanceIsItsCoppersRhoLengthOverSection() {
        CopperPaths.Path path = path("    track(top, width: 1, from: 0 0, to: 10 0)", 0, 0, 10, 0);
        // ρ at 25 °C, Ω·mm, times 10 mm over 1 mm × 0.035 mm.
        double expected = 1.724e-8 * 1000 * (1 + 0.00393 * 5) * 10 / 0.035;
        assertEquals(expected, path.resistance(), expected * 1e-6);
        assertEquals(path.resistance(), path.drop(), 1e-12);
    }

    @Test
    void isTheSameThroughLinesDrawnAsAShapeAsThroughATrackDrawnTheSame() {
        CopperPaths.Path track = path("    track(top, width: 1, from: 0 0, to: 10 0, to: 10 10)", 0, 0, 10, 10);
        CopperPaths.Path shape = path("    shape(top, width: 1, \"M0 0L10 0L10 10\")", 0, 0, 10, 10);
        assertTrue(track.resistance() > 0);
        assertEquals(track.resistance(), shape.resistance(), 1e-9);
        assertTrue(shape.flows().stream().allMatch(flow -> flow.branch().thing() == CopperPaths.Thing.SHAPE && flow.branch().index() == 0));
    }

    @Test
    void takesAFilledAreaAsOneConductorBetweenTheTracksThatReachIt() {
        CopperPaths.Path through = path("""
                    track(top, width: 1, from: 0 0, to: 6 0)
                    track(top, width: 1, from: 14 0, to: 20 0)
                    shape(top, filled, "M5 -5H15V5H5Z")
                """, 0, 0, 20, 0);
        CopperPaths.Path straight = path("    track(top, width: 1, from: 0 0, to: 20 0)", 0, 0, 20, 0);
        // Ten millimetres of the twenty are the area's, which costs nothing.
        assertEquals(straight.resistance() / 2, through.resistance(), 1e-6);
    }

    @Test
    void isRefusedWhereAShapeStandsOnAnotherLayer() {
        assertInstanceOf(CopperPaths.Refused.class, between("    shape(bottom, width: 1, \"M0 0L10 0\")", 0, 0, 10, 0));
    }

    @Test
    void aThinTrackIsOverloadedAndSaysTheWidthItNeeds() {
        CopperPaths.Path path = path("    track(top, width: 0.1, from: 0 0, to: 10 0)", 0, 0, 10, 0);
        CopperPaths.Flow weakest = path.weakest().orElseThrow();
        // 0.1 mm of 35 µm copper carries well under an ampere for 10 K.
        assertTrue(weakest.overloaded());
        double needed = weakest.neededWidth().orElseThrow();
        assertEquals(1, COPPER.capacityOf(needed * 0.035, 10, false), 1e-9);
    }

    @Test
    void aMicrostripOfFiftyOhmsOnAPcbOfOnePointSixIsAboutThreeMillimetresWide() {
        TrackStacks.Stack stack = assertInstanceOf(TrackStacks.Stands.class,
                STACKS.stackOf(List.of("top", "bottom"), VALUES, "top", Optional.empty(), Optional.empty())).stack();
        assertEquals(StackPhysics.TrackLine.MICROSTRIP, stack.line());
        double width = STACKS.widthFor(50, stack).orElseThrow();
        assertTrue(width > 2.7 && width < 3.2, "50 Ω over 1.6 mm FR-4 is " + width + " mm");
        assertEquals(50, STACKS.impedanceOf(width, stack).impedance(), 0.01);
    }

    @Test
    void anInnerTrackIsAStriplineAndAOneLayerPcbHasNoPlane() {
        TrackStacks.Stack stack = assertInstanceOf(TrackStacks.Stands.class,
                STACKS.stackOf(List.of("top", "inner-1", "inner-2", "bottom"), VALUES, "inner-1", Optional.empty(), Optional.empty())).stack();
        assertEquals(StackPhysics.TrackLine.STRIPLINE, stack.line());
        assertEquals(stack.permittivity(), STACKS.impedanceOf(0.2, stack).effectivePermittivity(), 1e-12);
        assertInstanceOf(TrackStacks.Unplaned.class, STACKS.stackOf(List.of("bottom"), VALUES, "bottom", Optional.empty(), Optional.empty()));
    }

    @Test
    void aPcbWarmsByThePowerOverWhatItsFacesGiveOff() {
        // 100 × 100 mm, both faces: 0.02 m² at 10 W/m²K — a watt warms it by 5 K.
        assertEquals(5, STACKS.warmth(1, 100, 100), 1e-9);
    }
}
