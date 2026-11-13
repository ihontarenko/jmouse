package org.jmouse.cad.library;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.cad.document.DocumentEditing;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnState;
import org.jmouse.cad.drawing.Holding;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CircuitLines;
import org.jmouse.cad.simulation.CircuitSheet;
import org.jmouse.el.grammar.ClauseLineNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How a drawn item stands — its group, its state — said in a drawing's text, read and written back the same. */
class ArrangementTest {

    private static PcbDrawing pcb(String body) {
        return PcbDrawing.of(CadLanguage.shipped().language().readOne("""
                pcb:panel-board("Panel board") {
                    circuit(nowhere)
                    layers(2)
                %s
                }
                """.formatted(body.indent(4)), "panel-board"));
    }

    @Test
    void readsAGroupAsAnItemOfItsOwnAndEachItemsGroupAndState() {
        PcbDrawing board = pcb("""
                group:front-panel(state: locked)
                place(DS1, at: 40 20, group: front-panel)
                place(SW1, at: 10 20, state: locked)
                place(R1, at: 60 20)
                track(top, width: 0.25, from: 0 0, to: 10 0, group: front-panel)
                via(at: 5 5, drill: 0.3, diameter: 0.6, state: locked)
                """);
        assertEquals(List.of(new DrawnGroup("front-panel", DrawnState.LOCKED)), board.groups());
        assertEquals(Arrangement.in("front-panel"), board.placements().get(0).arrangement());
        assertEquals(new Arrangement("", DrawnState.LOCKED), board.placements().get(1).arrangement());
        assertEquals(Arrangement.FREE, board.placements().get(2).arrangement());
        assertEquals(Arrangement.in("front-panel"), board.tracks().getFirst().arrangement());
        assertTrue(board.vias().getFirst().arrangement().locked());
    }

    @Test
    void holdsAnItemLockedItselfOrInALockedGroupAndNoOther() {
        PcbDrawing board = pcb("""
                group:front-panel(state: locked)
                group:spares()
                place(DS1, at: 40 20, group: front-panel)
                place(SW1, at: 10 20, state: locked)
                place(R1, at: 60 20, group: spares)
                place(R2, at: 70 20, group: unnamed)
                """);
        List<Boolean> held = board.placements().stream().map(placement -> board.drawnGroups().holds(placement.arrangement())).toList();
        assertEquals(List.of(true, true, false, false), held);
        assertEquals("its group 'front-panel' is locked", board.drawnGroups().holder(board.placements().getFirst().arrangement()).orElseThrow());
    }

    @Test
    void writesOnlyWhatIsNotTheDefaultAndTheGroupsBeforeTheirMembers() {
        PcbDrawing board = pcb("""
                place(R1, at: 60 20)
                place(DS1, at: 40 20, group: front-panel, state: locked)
                group:front-panel(state: locked)
                """);
        List<ClauseLineNode> lines = board.lines();
        List<String> words = lines.stream().map(line -> line.grammar().termWord()).filter(word -> word.equals("group") || word.equals("place")).toList();
        assertEquals(List.of("group", "place", "place"), words);
        ClauseLineNode free = lines.stream().filter(line -> "place".equals(line.word()) && line.name("reference").orElseThrow().equals("R1")).findFirst().orElseThrow();
        assertFalse(free.has(ArrangementLines.GROUP));
        assertFalse(free.has(ArrangementLines.STATE));
        ClauseLineNode held = lines.stream().filter(line -> "place".equals(line.word()) && line.name("reference").orElseThrow().equals("DS1")).findFirst().orElseThrow();
        assertEquals(new Arrangement("front-panel", DrawnState.LOCKED), ArrangementLines.arrangementOf(held));
    }

    @Test
    void refusesARevisionThatMovesOrRemovesWhatIsHeldAndNamesIt() {
        PcbDrawing before = pcb("""
                group:front-panel(state: locked)
                place(DS1, at: 40 20, group: front-panel)
                place(R1, at: 60 20)
                via(at: 5 5, drill: 0.3, diameter: 0.6, state: locked)
                """);
        PcbDrawing freeMoved = pcb("""
                group:front-panel(state: locked)
                place(DS1, at: 40 20, group: front-panel)
                place(R1, at: 70 20)
                via(at: 5 5, drill: 0.3, diameter: 0.6, state: locked)
                """);
        PcbDrawing heldMoved = pcb("""
                group:front-panel(state: locked)
                place(DS1, at: 45 20, group: front-panel)
                place(R1, at: 60 20)
                via(at: 5 5, drill: 0.3, diameter: 0.6, state: locked)
                """);
        PcbDrawing unlockedAndMoved = pcb("""
                place(DS1, at: 45 20, group: front-panel)
                place(R1, at: 60 20)
                via(at: 5 5, drill: 0.3, diameter: 0.6, state: locked)
                """);
        PcbDrawing viaRemoved = pcb("""
                group:front-panel(state: locked)
                place(DS1, at: 40 20, group: front-panel)
                place(R1, at: 60 20)
                """);
        assertTrue(Holding.refusal(before, freeMoved).isEmpty());
        assertEquals("DS1 is held where it stands — its group 'front-panel' is locked: unlock it before moving or removing it",
                Holding.refusal(before, heldMoved).orElseThrow());
        assertTrue(Holding.refusal(before, unlockedAndMoved).isEmpty());
        assertTrue(Holding.refusal(before, viaRemoved).orElseThrow().startsWith("the via at 5, 5 is held where it stands — it is locked"));
    }

    private static final String SHEET = """
            circuit:locked-sheet("Locked sheet") {
                part:R1(resistor, [terminal_one: a, terminal_two: 0], resistance: 1000)
                part:R2(resistor, [terminal_one: a, terminal_two: 0], resistance: 1000)
                group:front-panel(state: locked)
                place:R1(at: 100 100, group: front-panel)
                place:R2(at: 300 100)
                mark:ground1(ground, at: 100 200, state: locked)
            }
            """;

    private static Circuit circuit(String text) {
        return (Circuit) CadLanguage.shipped().modelOf(CadLanguage.shipped().language().readOne(text, "locked-sheet")).orElseThrow();
    }

    @Test
    void aSheetSaysItsGroupsAndHowEachOfItsItemsStands() {
        Circuit sheet = circuit(SHEET);
        assertEquals(List.of(new DrawnGroup("front-panel", DrawnState.LOCKED)), sheet.groups());
        assertEquals(List.of(Arrangement.in("front-panel"), Arrangement.FREE, new Arrangement("", DrawnState.LOCKED)),
                sheet.layout().stream().map(placement -> placement.arrangement()).toList());
        assertEquals(List.of(true, false, true), sheet.layout().stream().map(placement -> sheet.drawnGroups().holds(placement.arrangement())).toList());
    }

    @Test
    void aSheetWrittenAnewKeepsItsGroupsAndStatesAndRefusesMovingWhatIsHeld() {
        Circuit before = circuit(SHEET);
        DocumentNode document = CadLanguage.shipped().language().readOne(SHEET, "locked-sheet");
        List<Placement> layout = new ArrayList<>(before.layout());
        layout.set(1, layout.get(1).arranged(new Arrangement("", DrawnState.LOCKED)));
        CircuitSheet drawn = new CircuitSheet(before.parts(), layout, null, List.of(), null, List.of(new DrawnGroup("front-panel", DrawnState.FREE)));
        String written = new DocumentEditing(CadLanguage.shipped().language(), CadLanguage.shipped().modules())
                .redraw(document, drawn.redrawing(CircuitSheet.of(before), new CircuitLines(CadLanguage.shipped().language(), "locked-sheet")),
                        new DocumentEditing.Head("Locked sheet", null, null));
        Circuit after = circuit(written);
        assertEquals(List.of(new DrawnGroup("front-panel", DrawnState.FREE)), after.groups());
        assertEquals(new Arrangement("", DrawnState.LOCKED), after.layout().get(1).arrangement());
        assertTrue(Holding.refusal(before, after).isEmpty(), "nothing held was moved");
        String moved = SHEET.replace("place:R1(at: 100 100", "place:R1(at: 140 100");
        assertEquals("R1 is held where it stands — its group 'front-panel' is locked: unlock it before moving or removing it",
                Holding.refusal(before, circuit(moved)).orElseThrow());
    }
}
