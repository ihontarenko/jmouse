package org.jmouse.cad.panel;

import org.jmouse.cad.Point;
import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.node.Expression;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.jmouse.cad.panel.PanelDrawing.Board;
import org.jmouse.cad.panel.PanelDrawing.Frame;
import org.jmouse.cad.panel.PanelDrawing.Fiducial;
import org.jmouse.cad.panel.PanelDrawing.Tooling;

/** A panel document's lines read into a panel, and a line's class as its editor draws it. {@link PanelDocumentWriting} writes them. */
public final class PanelDocumentReading {

    private PanelDocumentReading() {
    }

    /** The panel a panel document's lines make. */
    static PanelDrawing read(DocumentNode panel) {
        List<Board> boards = new ArrayList<>();
        PanelSeparation separation = PanelSeparation.NONE;
        Frame frame = null;
        List<Fiducial> fiducials = new ArrayList<>();
        List<Tooling> tooling = new ArrayList<>();
        List<DrawnGroup> groups = new ArrayList<>();
        for (Expression statement : panel.getExpressions()) {
            if (!(statement instanceof ClauseLineNode line)) {
                continue;
            }
            // A group is named as every item is, by its term: it has no word of its own.
            if (line.grammar() == ArrangementLines.GROUP_LINE) {
                groups.add(ArrangementLines.groupOf(line));
                continue;
            }
            switch (line.word()) {
                case PanelWords.BOARD -> boards.add(boardOf(line));
                case PanelWords.SEPARATE -> separation = PanelSeparation.ofWord(line.name(PanelWords.KIND).orElseThrow());
                case PanelWords.FRAME -> frame = frameOf(line);
                case PanelWords.FIDUCIAL -> fiducials.add(new Fiducial(point(line), quantity(line, PanelWords.DIAMETER), quantity(line, PanelWords.OPENING)));
                case PanelWords.TOOLING -> tooling.add(new Tooling(point(line), quantity(line, PanelWords.DRILL)));
                default -> {
                    // A line another module's grammar owns is not the panel's to read.
                }
            }
        }
        return new PanelDrawing(boards, separation, Optional.ofNullable(frame), fiducials, tooling, groups);
    }

    private static Board boardOf(ClauseLineNode line) {
        Point at = point(line);
        return new Board(line.name(PanelWords.PCB).orElseThrow(), at.x(), at.y(), line.quantity(PanelWords.ROTATE).orElse(0.0),
                line.quantity(PanelWords.ACROSS).map(Double::intValue).orElse(1), line.quantity(PanelWords.DOWN).map(Double::intValue).orElse(1),
                quantity(line, PanelWords.GAP), ArrangementLines.arrangementOf(line));
    }

    private static Frame frameOf(ClauseLineNode line) {
        List<PanelSide> sides = line.names(PanelWords.SIDES).isEmpty() ? List.of(PanelSide.values()) : PanelSide.ofWords(line.names(PanelWords.SIDES));
        return new Frame(quantity(line, PanelWords.RAIL), sides, whole(line, PanelWords.FIDUCIALS), whole(line, PanelWords.TOOLING));
    }

    private static Point point(ClauseLineNode line) {
        PointNode at = line.point(PanelWords.AT).orElseThrow();
        return new Point(at.x(), at.y());
    }

    private static OptionalDouble quantity(ClauseLineNode line, String name) {
        return line.quantity(name).map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }

    private static OptionalInt whole(ClauseLineNode line, String name) {
        return line.quantity(name).map(value -> OptionalInt.of(value.intValue())).orElseGet(OptionalInt::empty);
    }
}
