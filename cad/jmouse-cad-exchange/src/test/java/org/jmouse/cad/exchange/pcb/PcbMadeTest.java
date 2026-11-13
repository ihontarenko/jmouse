package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.strokefont.StrokeFont;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A pcb as made, as the engine makes it for the editor's mask and its 3D view: bare copper as each way of saying it reaches
 * the same answer — a finish on a track, a window drawn on the mask as KiCad and Eagle draw one over a stretch of a track,
 * a part that brings its own window —, the copper a window leaves bare, the laminate it shows, and copper drawn as a shape.
 * Moved from the editor's own tests with the work itself on 2026-11-11: the editor draws the engine's.
 */
class PcbMadeTest {

    private static final PcbArtwork.Settings ARTWORK = settings();

    /** A footprint that brings its own window in the mask, and a line of silkscreen that is none. */
    private static final String WINDOWED = """
            footprint:windowed("Windowed") {
                outline(mask, filled, "M0 0H2V2H0Z")
                outline(silkscreen, width: 0.1, "M0 0H1")
            }
            """;

    private static PcbArtwork.Settings settings() {
        try (InputStream stream = PcbArtwork.class.getResourceAsStream(PcbArtwork.SECTION.resource())) {
            return JsonMapper.builder().build().readValue(stream, PcbArtwork.Settings.class);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private record Made(PcbArtwork artwork, ArtworkAreas areas, PcbSolid solid) {

        Geometry windows(String side) {
            return areas.areaOf(artwork.maskWindows(side));
        }
    }

    /** A pcb of 100 × 100 mm that lays out no circuit, with these lines. */
    private static Made made(String body) {
        DocumentNode footprint = CadLanguage.shipped().language().readOne(WINDOWED, "windowed");
        CadLibrary library = CadLibrary.library();
        CatalogueDocuments catalogue = slug -> slug.equals("windowed") ? Optional.of(footprint) : library.find(slug);
        DocumentNode pcb = CadLanguage.shipped().language().readOne("""
                pcb:made-probe("Made probe") {
                    layers(2)
                    edge("M-50 -50H50V50H-50Z")
                %s
                }
                """.formatted(body), "made-probe");
        PcbFabrication fabrication = PcbFabrication.of(pcb, catalogue);
        Map<String, Double> rules = CadStandards.shipped().pcbRules().valuesOf(fabrication.pcb().rules());
        PcbArtwork artwork = PcbArtwork.of(fabrication, rules, ARTWORK, StrokeFont.newStroke());
        ArtworkAreas areas = new ArtworkAreas(artwork.outlines(), ARTWORK.areas().steps());
        PcbSolid solid = PcbSolid.of(artwork, areas, PcbDrills.of(fabrication.pcb(), fabrication.footprints()), ARTWORK.areas().steps(),
                new PartBodies(ARTWORK.curveSteps()));
        return new Made(artwork, areas, solid);
    }

    private static PcbSolid.Side top(Made made) {
        return made.solid().side(PcbDrawing.TOP).orElseThrow();
    }

    @Test
    void copperUnderTheMaskOpensNoWindow() {
        assertTrue(made("    track(top, width: 2, from: 0 0, to: 20 0)").windows(PcbDrawing.TOP).isEmpty());
    }

    @Test
    void aFinishOpensTheMaskAlongItsCopperOnItsOwnSideOnly() {
        Made made = made("    track(top, width: 2, from: 0 0, to: 20 0, finish: bare)");
        // The track grown all round by the mask's expansion: 20 long, its round ends a circle.
        double half = 1 + ARTWORK.maskExpansion();
        assertEquals(20 * 2 * half + Math.PI * half * half, made.windows(PcbDrawing.TOP).getArea(), 0.05);
        assertTrue(made.windows(PcbDrawing.BOTTOM).isEmpty());
        assertEquals(made.solid().side(PcbDrawing.TOP).orElseThrow().copper().getArea(), top(made).bare().getArea(), 0.05);
    }

    @Test
    void whatThePcbDrawsOnTheMaskIsAWindowAnAreaOrALine() {
        Made area = made("    shape(top-mask, filled, \"M0 0H10V5H0Z\")");
        assertEquals(50, area.windows(PcbDrawing.TOP).getArea(), 0.05);
        Made line = made("    shape(top-mask, width: 1.5, \"M0 0H4\")\n    shape(top-silkscreen, width: 1.5, \"M0 10H4\")");
        assertEquals(4 * 1.5 + Math.PI * 0.75 * 0.75, line.windows(PcbDrawing.TOP).getArea(), 0.05);
    }

    @Test
    void aPartsWindowStandsWhereThePartStandsOnThePartsSide() {
        Made made = made("    place(U1, footprint: windowed, at: 10 20)");
        Geometry windows = made.windows(PcbDrawing.TOP);
        assertEquals(4, windows.getArea(), 1e-6);
        assertTrue(windows.contains(windows.getFactory().createPoint(new org.locationtech.jts.geom.Coordinate(11, 21))));
        assertTrue(made.windows(PcbDrawing.BOTTOM).isEmpty());
    }

    @Test
    void aWindowLeavesBareOnlyTheStretchOfATrackItCrosses() {
        Made made = made("    track(top, width: 2, from: 0 0, to: 20 0)\n    shape(top-mask, filled, \"M5 -5H10V5H5Z\")");
        // Five millimetres of a track two wide: not the whole of it, which is the point of drawing a window.
        assertEquals(10, top(made).bare().getArea(), 0.05);
        // The rest of the window shows the laminate.
        assertEquals(50 - 10, top(made).laminate().getArea(), 0.05);
    }

    @Test
    void aWindowOverBareBoardShowsTheLaminateOnThePcbOnly() {
        Made made = made("    shape(top-mask, filled, \"M40 -5H60V5H40Z\")");
        assertTrue(top(made).bare().isEmpty());
        // From x 40 to the pcb's edge at 50, ten high.
        assertEquals(100, top(made).laminate().getArea(), 0.05);
    }

    @Test
    void copperDrawnAsAShapeIsEtchedAsDrawn() {
        assertEquals(100, top(made("    shape(top, filled, \"M0 0H10V10H0Z\")")).copper().getArea(), 0.05);
        assertEquals(10 + Math.PI / 4, top(made("    shape(top, width: 1, \"M0 0H10\")")).copper().getArea(), 0.05);
    }
}
