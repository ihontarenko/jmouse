package org.jmouse.parallax.file;

import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.formula.el.WrittenStatement;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A line of a graph that is a parametric curve: the components of a curve as jMX writes them — {@code (cos(s), sin(s))}
 * — and the range of its variable as a jMX input is written — {@code s in 0 .. 2 pi}:
 * {@code (cos(s), sin(s))  s in 0 .. 2 pi}, or three components for a curve in space. Without the range the same
 * tuple is a point ({@link GeometryLines}). The bounds are numbers: a curve drawn up to a moving value is {@code until}.
 *
 * <p>The tuple and the range are jMF's grammar ({@link FormulaLanguage#written}). The graph's own definitions come first,
 * joined by {@code ;}, and are written in front of every component.</p>
 */
public final class ParametricLines {

    private ParametricLines() {
    }

    /** The line as a parametric curve, or empty when it is not `(components) name in a .. b`. */
    public static Optional<ObjectFiles.ReadObject> read(String source) {
        List<WrittenStatement> statements;
        try {
            statements = FormulaLanguage.standard().written(source);
        } catch (FormulaException unreadable) {
            return Optional.empty();
        }
        WrittenStatement last = statements.getLast();
        if (last.defines() || last.ranges().size() != 1 || last.ranges().getFirst().integer()) {
            return Optional.empty();
        }
        if (!(last.expression().shape() instanceof WrittenStatement.Tuple tuple)) {
            throw new IllegalArgumentException("A parametric curve is two or three components in parentheses — "
                    + "(cos s, sin s) —, not '" + last.expression().text() + "'.");
        }
        if (tuple.components().size() > 3) {
            throw new IllegalArgumentException("A parametric curve has two or three components, not "
                    + tuple.components().size() + ".");
        }
        WrittenStatement.Range range = last.ranges().getFirst();
        List<String> definitions = statements.subList(0, statements.size() - 1).stream()
                .filter(WrittenStatement::defines).map(WrittenStatement::text).toList();
        String prefix = definitions.isEmpty() ? "" : String.join("; ", definitions) + "; ";
        List<String> written = tuple.components().stream().map(component -> prefix + component.text()).toList();
        Interval bounds = Interval.of(bound(range.from()), bound(range.to()));
        ObjectDefinition.ParametricCurve curve = new ObjectDefinition.ParametricCurve(written, bounds, false,
                List.of(range.variable()));
        return Optional.of(new ObjectFiles.ReadObject("curve", 1, source, curve, parameters(written, range.variable())));
    }

    /** A bound is a number, `2 pi` included; a name a slider moves is refused with what to write instead. */
    private static double bound(String written) {
        FormulaLanguage language = FormulaLanguage.standard();
        try {
            boolean namesSlider = language.parse(written).variables().stream()
                    .anyMatch(name -> !ObjectFiles.isConstantName(name));
            if (namesSlider) {
                throw new IllegalArgumentException("The range of a curve is numbers, not '" + written
                        + "'; to draw it up to a moving value, write 'until' after it.");
            }
            return language.evaluate(written, Map.of());
        } catch (FormulaException refused) {
            throw new IllegalArgumentException(refused.getMessage(), refused);
        }
    }

    private static List<ParameterDefinition> parameters(List<String> components, String variable) {
        FormulaLanguage language = FormulaLanguage.standard();
        Set<String> names = new LinkedHashSet<>();
        for (String component : components) {
            try {
                names.addAll(language.parse(component).variables());
            } catch (FormulaException refused) {
                throw new IllegalArgumentException(refused.getMessage(), refused);
            }
        }
        names.remove(variable);
        return ObjectFiles.sliders(names);
    }
}
