package org.jmouse.cad.panel;

import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.cad.drawing.ArrangedDrawing;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnGroups;
import org.jmouse.cad.drawing.StandingItem;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.node.Expression;

import java.math.BigDecimal;
import java.util.ArrayList;
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
     * @param x        where the left of its outline stands, as it stands turned
     * @param y        where the top of its outline stands
     * @param rotation degrees, a quarter turn at a time
     * @param gap      between the copies of a grid; empty for the panel's usual
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

    /** @param diameter and {@code opening} empty for the usual */
    public record Fiducial(PcbDrawing.Point at, OptionalDouble diameter, OptionalDouble opening) {
    }

    /** @param drill empty for the usual */
    public record Tooling(PcbDrawing.Point at, OptionalDouble drill) {
    }

    /** The panel a panel document's lines make. */
    public static PanelDrawing of(DocumentNode panel) {
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
                case PanelLinesParser.BOARD -> boards.add(boardOf(line));
                case PanelLinesParser.SEPARATE -> separation = PanelSeparation.ofWord(line.name("kind").orElseThrow());
                case PanelLinesParser.FRAME -> frame = frameOf(line);
                case PanelLinesParser.FIDUCIAL -> fiducials.add(new Fiducial(point(line), quantity(line, "diameter"), quantity(line, "opening")));
                case PanelLinesParser.TOOLING -> tooling.add(new Tooling(point(line), quantity(line, "drill")));
                default -> {
                    // A line another module's grammar owns is not the panel's to read.
                }
            }
        }
        return new PanelDrawing(boards, separation, Optional.ofNullable(frame), fiducials, tooling, groups);
    }

    /** The panel's lines, as {@link #of} reads them. */
    public List<ClauseLineNode> lines() {
        List<ClauseLineNode> lines = new ArrayList<>();
        groups.forEach(group -> lines.add(ArrangementLines.groupLine(group)));
        for (Board board : boards) {
            LineBuilder line = line(PanelLinesParser.BOARD).slot("pcb", Nodes.word(board.pcb())).clause("at", Nodes.point(board.x(), board.y()));
            if (board.rotation() != 0) {
                line.clause("rotate", Nodes.number(board.rotation()));
            }
            if (board.across() != 1) {
                line.clause("across", Nodes.number(board.across()));
            }
            if (board.down() != 1) {
                line.clause("down", Nodes.number(board.down()));
            }
            board.gap().ifPresent(gap -> line.clause("gap", Nodes.number(gap)));
            ArrangementLines.write(line, board.arrangement());
            lines.add(line.build());
        }
        if (separation != PanelSeparation.NONE) {
            lines.add(line(PanelLinesParser.SEPARATE).slot("kind", Nodes.word(separation.word())).build());
        }
        frame.ifPresent(given -> {
            LineBuilder line = line(PanelLinesParser.FRAME);
            given.rail().ifPresent(rail -> line.clause("rail", Nodes.number(rail)));
            line.clause("sides", Nodes.words(given.sides().stream().map(PanelSide::word).toList()));
            given.fiducials().ifPresent(count -> line.clause("fiducials", Nodes.number(count)));
            given.tooling().ifPresent(count -> line.clause("tooling", Nodes.number(count)));
            lines.add(line.build());
        });
        for (Fiducial fiducial : fiducials) {
            LineBuilder line = line(PanelLinesParser.FIDUCIAL).clause("at", Nodes.point(fiducial.at().x(), fiducial.at().y()));
            fiducial.diameter().ifPresent(diameter -> line.clause("diameter", Nodes.number(diameter)));
            fiducial.opening().ifPresent(opening -> line.clause("opening", Nodes.number(opening)));
            lines.add(line.build());
        }
        for (Tooling hole : tooling) {
            LineBuilder line = line(PanelLinesParser.TOOLING).clause("at", Nodes.point(hole.at().x(), hole.at().y()));
            hole.drill().ifPresent(drill -> line.clause("drill", Nodes.number(drill)));
            lines.add(line.build());
        }
        return lines;
    }

    private static LineBuilder line(String word) {
        return LineBuilder.line(PanelLinesParser.grammar(word));
    }

    private static Board boardOf(ClauseLineNode line) {
        PcbDrawing.Point at = point(line);
        return new Board(line.name("pcb").orElseThrow(), at.x(), at.y(), line.quantity("rotate").orElse(0.0),
                line.quantity("across").map(Double::intValue).orElse(1), line.quantity("down").map(Double::intValue).orElse(1),
                quantity(line, "gap"), ArrangementLines.arrangementOf(line));
    }

    private static Frame frameOf(ClauseLineNode line) {
        List<PanelSide> sides = line.names("sides").isEmpty() ? List.of(PanelSide.values()) : PanelSide.ofWords(line.names("sides"));
        return new Frame(quantity(line, "rail"), sides, whole(line, "fiducials"), whole(line, "tooling"));
    }

    private static PcbDrawing.Point point(ClauseLineNode line) {
        PointNode at = line.point("at").orElseThrow();
        return new PcbDrawing.Point(at.x(), at.y());
    }

    private static OptionalDouble quantity(ClauseLineNode line, String name) {
        return line.quantity(name).map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }

    private static OptionalInt whole(ClauseLineNode line, String name) {
        return line.quantity(name).map(value -> OptionalInt.of(value.intValue())).orElseGet(OptionalInt::empty);
    }
}
