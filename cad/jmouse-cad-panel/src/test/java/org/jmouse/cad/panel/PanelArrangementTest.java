package org.jmouse.cad.panel;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnState;
import org.jmouse.cad.drawing.Holding;
import org.jmouse.cad.library.CadLanguage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A panel's boards grouped and locked: read, written back the same, and a revision moving a held one refused. */
class PanelArrangementTest {

    private static final DocumentLanguage LANGUAGE = language();

    private static DocumentLanguage language() {
        List<DocumentModule> modules = new ArrayList<>(CadLanguage.engineModules());
        modules.add(new PanelModule());
        return CadLanguage.of(modules).language();
    }

    private static PanelDrawing panel(String lines) {
        return PanelDrawing.of(LANGUAGE.readOne("panel:run(\"Run\") {\n" + lines + "\n}\n", "run"));
    }

    private static final String HELD = """
            group:fixture(state: locked)
            board(main-pcb, at: 0 0, group: fixture)
            board(main-pcb, at: 60 0, state: locked)
            board(spare-pcb, at: 120 0)
            """;

    @Test
    void readsItsGroupsAndHowEachBoardStands() {
        PanelDrawing read = panel(HELD);
        assertEquals(List.of(new DrawnGroup("fixture", DrawnState.LOCKED)), read.groups());
        assertEquals(List.of(Arrangement.in("fixture"), new Arrangement("", DrawnState.LOCKED), Arrangement.FREE),
                read.boards().stream().map(PanelDrawing.Board::arrangement).toList());
        PanelDrawing written = PanelDrawing.of(LANGUAGE.readOne(LANGUAGE.write(panelNode(read)), "run"));
        assertEquals(read.groups(), written.groups());
        assertEquals(read.boards(), written.boards());
    }

    @Test
    void refusesMovingAHeldBoardAndLetsAFreeOneMove() {
        PanelDrawing before = panel(HELD);
        assertTrue(Holding.refusal(before, panel(HELD.replace("spare-pcb, at: 120 0", "spare-pcb, at: 130 0"))).isEmpty());
        assertEquals("the board main-pcb at 60, 0 is held where it stands — it is locked: unlock it before moving or removing it",
                Holding.refusal(before, panel(HELD.replace("main-pcb, at: 60 0", "main-pcb, at: 70 0"))).orElseThrow());
    }

    /** The panel as a document of its lines, as the editor's text is written from them. */
    private static DocumentNode panelNode(PanelDrawing drawing) {
        DocumentNode node = LANGUAGE.readOne("panel:run(\"Run\") {\n}\n", "run");
        drawing.lines().forEach(node::addExpression);
        return node;
    }
}
