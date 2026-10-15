package org.jmouse.parallax.file;

import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A line of a graph that is a parametric curve: the components of a curve as jMX writes them — {@code (cos(s), sin(s))}
 * — and the range of its variable as a jMX input is written — {@code s in 0 .. 2 pi}:
 * {@code (cos(s), sin(s))  s in 0 .. 2 pi}, or three components for a curve in space. Without the range the same
 * tuple is a point ({@link GeometryLines}). The bounds are numbers: a curve drawn up to a moving value is {@code until}.
 *
 * <p>The graph's own definitions come first, joined by {@code ;}, and are written in front of every component.</p>
 */
public final class ParametricLines {

    private static final Pattern LINE = Pattern.compile("\\s*(\\(.*\\))\\s+([A-Za-z][A-Za-z0-9_]*)\\s+in\\s+(.+?)\\s*\\.\\.\\s*(.+?)\\s*",
            Pattern.DOTALL);
    private static final Pattern DEFINITION = Pattern.compile("\\s*[A-Za-z][A-Za-z0-9_]*\\s*(\\([^)]*\\))?\\s*=(?!=).*", Pattern.DOTALL);

    private ParametricLines() {
    }

    /** The line as a parametric curve, or empty when it is not `(components) name in a .. b`. */
    public static Optional<ObjectFiles.ReadObject> read(String source) {
        List<String> statements = ObjectFiles.splitTopLevel(source, ';');
        if (statements.isEmpty()) {
            return Optional.empty();
        }
        Matcher line = LINE.matcher(statements.getLast());
        if (!line.matches()) {
            return Optional.empty();
        }
        String tuple = line.group(1);
        List<String> components = ObjectFiles.splitTopLevel(tuple.substring(1, tuple.length() - 1), ',');
        if (components.size() < 2 || components.size() > 3) {
            throw new IllegalArgumentException("A parametric curve has two or three components, not " + components.size() + ".");
        }
        String variable = line.group(2);
        List<String> definitions = statements.subList(0, statements.size() - 1).stream()
                .filter(statement -> DEFINITION.matcher(statement).matches()).toList();
        String prefix = definitions.isEmpty() ? "" : String.join("; ", definitions) + "; ";
        List<String> written = components.stream().map(component -> prefix + component).toList();
        Interval range = Interval.of(bound(line.group(3)), bound(line.group(4)));
        ObjectDefinition.ParametricCurve curve = new ObjectDefinition.ParametricCurve(written, range, false, List.of(variable));
        return Optional.of(new ObjectFiles.ReadObject("curve", 1, source, curve, parameters(written, variable)));
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
