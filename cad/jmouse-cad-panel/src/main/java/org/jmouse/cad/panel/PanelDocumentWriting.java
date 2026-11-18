package org.jmouse.cad.panel;

import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import java.util.ArrayList;
import java.util.List;
import org.jmouse.cad.panel.PanelDrawing.Board;
import org.jmouse.cad.panel.PanelDrawing.Fiducial;
import org.jmouse.cad.panel.PanelDrawing.Tooling;

/** A panel written as its document's lines, built on the grammars that read them. {@link PanelDocumentReading} reads them back. */
public final class PanelDocumentWriting {

    private PanelDocumentWriting() {
    }

    /** The panel's lines, as {@link #of} reads them. */
    static List<ClauseLineNode> written(PanelDrawing drawing) {
        List<ClauseLineNode> lines = new ArrayList<>();
        drawing.groups().forEach(group -> lines.add(ArrangementLines.groupLine(group)));
        for (Board board : drawing.boards()) {
            LineBuilder line = line(PanelWords.BOARD).slot(PanelWords.PCB, Nodes.word(board.pcb())).clause(PanelWords.AT, Nodes.point(board.x(), board.y()));
            if (board.rotation() != 0) {
                line.clause(PanelWords.ROTATE, Nodes.number(board.rotation()));
            }
            if (board.across() != 1) {
                line.clause(PanelWords.ACROSS, Nodes.number(board.across()));
            }
            if (board.down() != 1) {
                line.clause(PanelWords.DOWN, Nodes.number(board.down()));
            }
            board.gap().ifPresent(gap -> line.clause(PanelWords.GAP, Nodes.number(gap)));
            ArrangementLines.write(line, board.arrangement());
            lines.add(line.build());
        }
        if (drawing.separation() != PanelSeparation.NONE) {
            lines.add(line(PanelWords.SEPARATE).slot(PanelWords.KIND, Nodes.word(drawing.separation().word())).build());
        }
        drawing.frame().ifPresent(given -> {
            LineBuilder line = line(PanelWords.FRAME);
            given.rail().ifPresent(rail -> line.clause(PanelWords.RAIL, Nodes.number(rail)));
            line.clause(PanelWords.SIDES, Nodes.words(given.sides().stream().map(PanelSide::word).toList()));
            given.fiducials().ifPresent(count -> line.clause(PanelWords.FIDUCIALS, Nodes.number(count)));
            given.tooling().ifPresent(count -> line.clause(PanelWords.TOOLING, Nodes.number(count)));
            lines.add(line.build());
        });
        for (Fiducial fiducial : drawing.fiducials()) {
            LineBuilder line = line(PanelWords.FIDUCIAL).clause(PanelWords.AT, Nodes.point(fiducial.at().x(), fiducial.at().y()));
            fiducial.diameter().ifPresent(diameter -> line.clause(PanelWords.DIAMETER, Nodes.number(diameter)));
            fiducial.opening().ifPresent(opening -> line.clause(PanelWords.OPENING, Nodes.number(opening)));
            lines.add(line.build());
        }
        for (Tooling hole : drawing.tooling()) {
            LineBuilder line = line(PanelWords.TOOLING).clause(PanelWords.AT, Nodes.point(hole.at().x(), hole.at().y()));
            hole.drill().ifPresent(drill -> line.clause(PanelWords.DRILL, Nodes.number(drill)));
            lines.add(line.build());
        }
        return lines;
    }

    private static LineBuilder line(String word) {
        return LineBuilder.line(PanelLinesParser.grammar(word));
    }
}
