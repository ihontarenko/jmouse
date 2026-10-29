package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Given;
import org.jmouse.cad.component.State;
import org.jmouse.cad.simulation.ModelLanguage;
import org.jmouse.formula.RealCompiler;

import java.util.Arrays;
import java.util.Set;

/**
 * Where a state starts, as the engine holds it: a number — an expression jMF folds, {@code 0}, {@code -1} —, or a
 * logic level no number says, {@code X} (unknown) or {@code Z} (released). A row of states starts every slot there.
 * A whole number is held as one, as the behaviour that reads a discrete state counts with it.
 */
final class StateStart {

    /** The levels a state may start at that are no number: unknown, and released. */
    private static final Set<String> LEVELS = Set.of("X", "Z");

    /** The largest whole number held as one rather than as a fraction. */
    private static final double LARGEST_WHOLE = 1e9;

    private StateStart() {
    }

    /** Where the state starts: a number, a level's word, or — a row — a slot of either for each of its slots. */
    static Object of(State state, String where) {
        Object value = value(state.start(), where + ", state " + state.name());
        if (state.index() == null) {
            return value;
        }
        Object[] row = new Object[state.index().size()];
        Arrays.fill(row, value);
        return row;
    }

    /** As a continuous state reads it: a level that is no number stands halfway. */
    static double numeric(Object value) {
        return UnitArguments.numeric(value);
    }

    private static Object value(Given given, String where) {
        if (!(given instanceof Formula written)) {
            throw new IllegalArgumentException(where + ": a state starts at a number or a level, not at a text");
        }
        org.jmouse.formula.Formula parsed = ModelLanguage.LANGUAGE.parse(written.written());
        if (parsed instanceof org.jmouse.formula.Formula.Variable variable && LEVELS.contains(variable.name())) {
            return variable.name();
        }
        double number;
        try {
            number = RealCompiler.fold(parsed);
        } catch (RuntimeException unfolded) {
            throw new IllegalArgumentException(where + ": '" + written.written() + "' is no start; write a number, X or Z", unfolded);
        }
        boolean whole = number == Math.rint(number) && Math.abs(number) < LARGEST_WHOLE;
        return whole ? (Object) (int) number : (Object) number;
    }
}
