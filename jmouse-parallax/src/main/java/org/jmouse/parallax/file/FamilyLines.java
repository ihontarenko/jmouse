package org.jmouse.parallax.file;

import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.formula.el.WrittenStatement;

import java.util.List;
import java.util.Optional;

/**
 * A graph line that is a family: any line followed by the range of a whole index, as jMX writes an integer input —
 * {@code y = sin((2k - 1) x) / (2k - 1)  k in 1 .. N integer}. What comes before the range is read as it would be
 * alone (a curve, a parametric curve, a point, a shape) with the index as one of its parameters; the family is that
 * member once for every whole value of the index. The range is jMF's grammar ({@link FormulaLanguage#written}).
 */
public final class FamilyLines {

    /** The member's text, the index and its bounds as written. */
    public record Split(String member, String index, String from, String to) {
    }

    private FamilyLines() {
    }

    /** The line without its range, and the range; empty when the line ends with no `name in a .. b integer`. */
    public static Optional<Split> split(String source) {
        List<WrittenStatement> statements;
        try {
            statements = FormulaLanguage.standard().written(source);
        } catch (FormulaException unreadable) {
            return Optional.empty();
        }
        List<WrittenStatement.Range> ranges = statements.getLast().ranges();
        if (ranges.isEmpty() || !ranges.getLast().integer()) {
            return Optional.empty();
        }
        WrittenStatement.Range index = ranges.getLast();
        // The member is the line as it was written up to its index's range: the statements before the last, the last
        // one and any range of its own — `(cos(k s), sin(k s))  s in 0 .. 2 pi` of a family of curves.
        String member = source.substring(0, index.start()).stripTrailing();
        return Optional.of(new Split(member, index.variable(), index.from(), index.to()));
    }
}
