package org.jmouse.cad.panel;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentFlattening;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a panel comes to, read from panels in the language. */
class PanelFlatteningTest {

    private static final PanelDefaults DEFAULTS = new PanelDefaults(5, 2, 1, 2, 3.2, 5, 40, 0.5, 0.8);
    private static final PanelFlattening FLATTENING = new PanelFlattening(() -> DEFAULTS, CadStandards.shipped());
    private static final DocumentLanguage LANGUAGE = language();

    private static DocumentLanguage language() {
        List<DocumentModule> modules = new ArrayList<>(CadLanguage.engineModules());
        modules.add(new PanelModule());
        return CadLanguage.of(modules).language();
    }

    private static final Map<String, String> DOCUMENTS = Map.ofEntries(
            Map.entry("circuit-a", "circuit:circuit-a(\"A\") {\n}\n"),
            Map.entry("square", """
                    pcb:square("Square") {
                        circuit(circuit-a)
                        edge("M0 0H20V10H0Z")
                        track(top, width: 0.2, from: 1 1, to: 5 1)
                        hole(at: 2 2, drill: 1)
                        zone(top, net: GND, "M1 1H9V9H1Z")
                    }
                    """),
            Map.entry("circuit-parts", """
                    circuit:circuit-parts("With a part") {
                            part:R1(resistor, [a: IN, b: 0], value: "10 kΩ")
                    }
                    """),
            Map.entry("with-part", """
                    pcb:with-part("With a part") {
                        circuit(circuit-parts)
                        edge("M0 0H20V10H0Z")
                        place(R1, at: 5 5)
                    }
                    """),
            Map.entry("other", """
                    pcb:other("Other") {
                        circuit(circuit-a)
                        layers(4)
                        edge("M0 0H20V10H0Z")
                    }
                    """),
            Map.entry("notched", """
                    pcb:notched("Notched") {
                        circuit(circuit-a)
                        edge("M0 0H20V10H10V5H0Z")
                    }
                    """));

    private static final CatalogueDocuments CATALOGUE = slug -> Optional.ofNullable(DOCUMENTS.get(slug)).map(text -> LANGUAGE.readOne(text, slug));

    private static DocumentFlattening.Flattened flat(String lines) {
        return FLATTENING.flatten(LANGUAGE.readOne("panel:run(\"Run\") {\n" + lines + "\n}\n", "run"), CATALOGUE);
    }

    private static PcbDrawing pcb(String lines) {
        return PcbDrawing.of(flat(lines).document());
    }

    @Test
    void copiesStandInAGridAndTheirCopperMovesWithThem() {
        PcbDrawing pcb = pcb("board(square, at: 100 200, across: 2, gap: 3)\nframe(rail: 5)");
        assertEquals(List.of(new Point(101, 201), new Point(105, 201)), pcb.tracks().getFirst().points());
        assertEquals(new Point(124, 201), pcb.tracks().get(1).points().getFirst(), "20 wide and a gap of 3 on");
        assertEquals(102, pcb.holes().getFirst().x(), 1e-9);
        assertEquals(125, pcb.holes().get(1).x(), 1e-9);
    }

    @Test
    void aQuarterTurnTurnsTheBoardAndStillStandsWhereItIsTold() {
        PcbDrawing pcb = pcb("board(square, at: 0 0, rotate: 90)");
        // Counter-clockwise as seen, Y down: (x, y) goes to (y, -x), then moved so its bounds start at the origin.
        assertEquals(new Point(1, 19), pcb.tracks().getFirst().points().getFirst());
        assertEquals(new Point(1, 15), pcb.tracks().getFirst().points().get(1));
    }

    @Test
    void netsOfCopiesAreToldApart() {
        PcbDrawing pcb = pcb("board(square, at: 0 0, across: 2, gap: 0)\nseparate(v-score)");
        assertEquals(List.of("B1_GND", "B2_GND"), pcb.zones().stream().map(Zone::net).toList());
    }

    @Test
    void differentBoardsShareAPanelButOneMaterial() {
        PcbDrawing mixed = pcb("board(square, at: 0 0)\nboard(notched, at: 30 0)\nframe(rail: 5)\nseparate(mouse-bites)");
        assertEquals(1, mixed.holes().stream().filter(hole -> hole.drill() == 1).count(), "the square's own hole");
        assertTrue(mixed.holes().stream().anyMatch(hole -> hole.drill() == 0.5), "tabs hold both, the notched one where its edge really is");
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> pcb("board(square, at: 0 0)\nboard(other, at: 30 0)\nframe(rail: 5)"));
        assertTrue(refused.getMessage().contains("copper layers"), refused.getMessage());
    }

    @Test
    void theFrameSetsItsHolesAndFiducialsOnItsRails() {
        PcbDrawing pcb = pcb("board(square, at: 0 0)\nframe(rail: 5, sides: [top, bottom], fiducials: 3, tooling: 4)");
        assertEquals(4, pcb.holes().stream().filter(hole -> hole.drill() == 3.2).count());
        assertEquals(new Hole(2.5, -2.5, 3.2, 0), pcb.holes().stream().filter(hole -> hole.drill() == 3.2).findFirst().orElseThrow());
        assertEquals(3 * 4, pcb.shapes().size(), "copper and opening, on both faces");
    }

    @Test
    void vScoreRunsRightAcrossAndRefusesWhatItWouldCut() {
        PcbDrawing pcb = pcb("board(square, at: 0 0, across: 2, down: 2)\nseparate(v-score)\nframe(rail: 5, sides: [top, bottom])");
        assertEquals(3 + 3, pcb.shapes().stream().filter(shape -> shape.layer().equals("documentation")).count());
        assertThrows(IllegalArgumentException.class, () -> pcb("board(square, at: 0 0)\nboard(square, at: 5 10)\nseparate(v-score)"));
        assertThrows(IllegalArgumentException.class, () -> pcb("board(notched, at: 0 0)\nseparate(v-score)"));
    }

    @Test
    void mouseBitesLeaveTabsAndRowsOfHoles() {
        PcbDrawing pcb = pcb("board(square, at: 0 0, across: 2)\nseparate(mouse-bites)\nframe(rail: 5)");
        assertTrue(pcb.holes().stream().anyMatch(hole -> hole.drill() == 0.5), "a tab's row");
        assertTrue(pcb.outline().contains("Z"), pcb.outline());
    }

    @Test
    void whatWouldFallApartOrOverlapIsRefused() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> pcb("board(square, at: 0 0)\nboard(square, at: 50 0)")).getMessage().contains("separate pieces"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> pcb("board(square, at: 0 0)\nboard(square, at: 5 0)\nframe(rail: 5)")).getMessage().contains("on one another"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> pcb("board(square, at: 0 0, rotate: 45)")).getMessage().contains("quarter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> pcb("board(nothing, at: 0 0)")).getMessage().contains("no pcb"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> pcb("frame(rail: 5)")).getMessage().contains("no board"));
    }

    @Test
    void everyCopysPartsArriveToldApartAndPrintingWhatTheBoardPrints() {
        DocumentFlattening.Flattened flat = flat("board(with-part, at: 0 0, across: 2)\nframe(rail: 5)\nseparate(mouse-bites)");
        PcbFabrication fabrication = PcbFabrication.of(flat.document(), flat.catalogue());
        assertEquals(List.of(), fabrication.notes(), "no part left out");
        assertEquals(List.of("B1_R1", "B2_R1"), fabrication.parts().stream().map(PcbFabrication.SeatedPart::designator).toList());
        assertEquals(List.of("R1", "R1"), fabrication.parts().stream().map(PcbFabrication.SeatedPart::printed).toList());
        assertEquals(List.of("10 kΩ", "10 kΩ"), fabrication.parts().stream().map(PcbFabrication.SeatedPart::value).toList());
        assertEquals(new Point(27, 5), new Point(fabrication.parts().get(1).placement().x(), fabrication.parts().get(1).placement().y()), "20 wide and a gap of 2 on");
    }

    @Test
    void aPanelIsWrittenAsItIsRead() {
        DocumentNode read = LANGUAGE.readOne("""
                panel:run("Run") {
                    board(square, at: 1 2, rotate: 90, across: 2, down: 3, gap: 1.5)
                    separate(mouse-bites)
                    frame(rail: 4, sides: [top, left], fiducials: 2, tooling: 1)
                    fiducial(at: 3 4, diameter: 1.2, opening: 2.2)
                    tooling(at: 5 6, drill: 3)
                }
                """, "run");
        PanelDrawing panel = PanelDrawing.of(read);
        DocumentNode written = new DocumentNode(PanelModule.PANEL, "Run", "run", null, 1);
        panel.lines().forEach(written::addExpression);
        assertEquals(panel, PanelDrawing.of(LANGUAGE.readOne(LANGUAGE.write(written), "run")));
    }

    @Test
    void aFlatPanelIsAPcbEveryFormatCanRead() {
        DocumentFlattening.Flattened flat = flat("board(square, at: 0 0, across: 2)\nframe(rail: 5)\nseparate(mouse-bites)");
        DocumentNode written = LANGUAGE.readOne(LANGUAGE.write(flat.document()), "run");
        assertEquals("run", written.slug());
        PcbFabrication fabrication = PcbFabrication.of(written, flat.catalogue());
        assertEquals("run", fabrication.name());
        assertFalse(PcbDrawing.of(written).outline().isBlank());
    }
}
