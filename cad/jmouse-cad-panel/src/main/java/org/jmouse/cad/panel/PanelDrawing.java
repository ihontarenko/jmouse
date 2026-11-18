package org.jmouse.cad.panel;

import org.jmouse.cad.Point;
import org.jmouse.cad.drawing.ArrangedDrawing;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnGroups;
import org.jmouse.cad.drawing.StandingItem;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.ClauseLineNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * A panel as its lines say it: which boards stand where, how they come apart, its frame and what stands on the frame.
 *
 * @param boards     boards, of one pcb or several
 * @param separation how they come apart
 * @param frame      the rails round them; empty for none
 * @param fiducials  fiducials set by hand, besides the frame's
 * @param tooling    tooling holes set by hand, besides the frame's
 * @param groups     the groups its boards are chosen and moved with, each with its own state
 */
public record PanelDrawing(List<Board> boards, PanelSeparation separation, Optional<Frame> frame, List<Fiducial> fiducials,
                           List<Tooling> tooling, List<DrawnGroup> groups) implements ArrangedDrawing {

    public PanelDrawing {
        boards = List.copyOf(boards);
        fiducials = List.copyOf(fiducials);
        tooling = List.copyOf(tooling);
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    /** Its boards as a lock looks at them: each named by its pcb and where it stands, apart from how it stands. */
    @Override
    public List<StandingItem> standingItems() {
        return boards.stream().map(board -> new StandingItem("the board " + board.pcb() + " at " + millimetres(board.x()) + ", " + millimetres(board.y()),
                board.arranged(Arrangement.FREE), board.arrangement())).toList();
    }

    @Override
    public DrawnGroups drawnGroups() {
        return new DrawnGroups(groups);
    }

    private static String millimetres(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /**
     * A pcb standing on the panel, once or in a grid.
     *
     * @param pcb         the pcb it stands, by slug
     * @param x           where the left of its outline stands, as it stands turned
     * @param y           where the top of its outline stands
     * @param rotation    degrees, a quarter turn at a time
     * @param across      how many copies stand side by side
     * @param down        how many copies stand one under another
     * @param gap         between the copies of a grid; empty for the panel's usual
     * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
     */
    public record Board(String pcb, double x, double y, double rotation, int across, int down, OptionalDouble gap, Arrangement arrangement) {

        public Board {
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        /** The same board standing otherwise among the others. */
        public Board arranged(Arrangement other) {
            return new Board(pcb, x, y, rotation, across, down, gap, other);
        }
    }

    /**
     * Rails round the boards.
     *
     * @param rail      how wide; empty for the usual
     * @param sides     which have one
     * @param fiducials how many fiducials the frame sets by itself
     * @param tooling   how many tooling holes the frame sets by itself
     */
    public record Frame(OptionalDouble rail, List<PanelSide> sides, OptionalInt fiducials, OptionalInt tooling) {

        public Frame {
            sides = List.copyOf(sides);
        }
    }

    /**
     * A fiducial set by hand: a mark a machine finds the panel by.
     *
     * @param at       where it stands, millimetres
     * @param diameter its copper's diameter, millimetres; empty for the usual
     * @param opening  the solder mask's opening round it, millimetres; empty for the usual
     */
    public record Fiducial(Point at, OptionalDouble diameter, OptionalDouble opening) {
    }

    /**
     * A tooling hole set by hand: a hole a machine holds the panel by.
     *
     * @param at    where it stands, millimetres
     * @param drill its diameter, millimetres; empty for the usual
     */
    public record Tooling(Point at, OptionalDouble drill) {
    }

    /**
     * The panel as its document's lines, built on the grammars that read them.
     *
     * @throws IllegalArgumentException when a path does not read as path data
     */
    public List<ClauseLineNode> lines() {
        return PanelDocumentWriting.written(this);
    }

    /** The panel a panel document's lines make. */
    public static PanelDrawing of(DocumentNode document) {
        return PanelDocumentReading.read(document);
    }
}
