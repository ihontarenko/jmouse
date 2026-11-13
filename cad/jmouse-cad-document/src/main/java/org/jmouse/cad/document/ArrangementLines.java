package org.jmouse.cad.document;

import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnState;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.Nodes;

import java.util.function.UnaryOperator;

import static org.jmouse.el.grammar.Values.WORD;

/**
 * How every drawing language says how its items stand — a sheet's parts, a pcb's footprints, tracks, arcs and vias, a panel's
 * boards —, said once: the clauses {@code group: front-panel} and {@code state: locked} on an item's line, and a group's own line,
 * {@code group:front-panel(state: locked)}. A line that says neither is free and in no group, and is written so again.
 */
public final class ArrangementLines {

    /** The clause naming the group an item is chosen and moved with — and the word of a group's own line. */
    public static final String GROUP = "group";

    /** The clause saying an item's or a group's state. */
    public static final String STATE = "state";

    /** A group's own line, named as every item of a drawing is — {@code group:front-panel(state: locked)} —: its state when it is locked. */
    public static final LineGrammar GROUP_LINE = LineGrammar.named(GROUP, "name")
            .optional(STATE, WORD, DrawnState.words().toArray(String[]::new))
            .build();

    /** The clauses an item's line takes, handed to its grammar with {@code including}. */
    public static final UnaryOperator<LineGrammar.Builder> CLAUSES = line -> line
            .optional(GROUP, WORD)
            .optional(STATE, WORD, DrawnState.words().toArray(String[]::new));

    private ArrangementLines() {
    }

    /** How the item a line writes stands: its group and its state, free and in none when it says neither. */
    public static Arrangement arrangementOf(ClauseLineNode line) {
        return new Arrangement(line.word(GROUP).orElse(""), stateOf(line));
    }

    /** Writes how an item stands onto its line: only what is not the default, so a free item in no group writes nothing. */
    public static void write(LineBuilder line, Arrangement arrangement) {
        if (arrangement.grouped()) {
            line.clause(GROUP, Nodes.word(arrangement.group()));
        }
        writeState(line, arrangement.state());
    }

    /** A group as its line declares it. */
    public static DrawnGroup groupOf(ClauseLineNode line) {
        return new DrawnGroup(line.name("name").orElseThrow(), stateOf(line));
    }

    /** A group's own line. */
    public static ClauseLineNode groupLine(DrawnGroup group) {
        LineBuilder line = LineBuilder.line(GROUP_LINE).slot("name", Nodes.word(group.name()));
        writeState(line, group.state());
        return line.build();
    }

    private static DrawnState stateOf(ClauseLineNode line) {
        return line.word(STATE).flatMap(DrawnState::of).orElse(DrawnState.FREE);
    }

    private static void writeState(LineBuilder line, DrawnState state) {
        if (state != DrawnState.FREE) {
            line.clause(STATE, Nodes.word(state.word()));
        }
    }
}
