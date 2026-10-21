package org.jmouse.parallax.file;

import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.formula.el.WrittenStatement;
import org.jmouse.formula.el.WrittenStatement.Written;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.GeometricElement;
import org.jmouse.parallax.object.ObjectDefinition.GeometricElementType;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A line of a graph that is geometry rather than a function: a point written as a tuple — {@code (cos t, sin t)}, the
 * notation a curve's components already have in jMX — or a step of a construction's own vocabulary —
 * {@code vector((0, 0), P)}, {@code segment(A, B)}, {@code circle(O, A)}. It is read as a small construction, so the
 * construction engine computes and draws it; nothing new is computed for the graph.
 *
 * <p>Labels say what is marked: a named element carries its name, a bare point a blank label (marked, no word), and
 * the helpers a call builds — the tail of a vector, the line itself when it is a shape — an empty one (not marked).</p>
 *
 * <p>The line comes as the graph writes it: its definitions first, then the line, joined by {@code ;}; jMF's grammar
 * reads it ({@link FormulaLanguage#written}). A definition {@code P = (…)} or {@code S = segment(…)} is an element the
 * line may name; any other definition — a function, a constant — is kept as jMF and written in front of every
 * coordinate, so a coordinate may call it.</p>
 */
public final class GeometryLines {

    private static final Set<String> STEP_WORDS = wordsOf();

    private GeometryLines() {
    }

    /** The line as a construction, or empty when its last statement is not a point or a construction step. */
    public static Optional<ObjectFiles.ReadObject> read(String source) {
        List<WrittenStatement> statements;
        try {
            statements = FormulaLanguage.standard().written(source);
        } catch (FormulaException unreadable) {
            return Optional.empty();
        }
        WrittenStatement last = statements.getLast();
        if (!last.ranges().isEmpty() || !isGeometry(last.expression())) {
            return Optional.empty();
        }
        String lastName = isElement(last) ? last.name().orElseThrow() : null;
        if (last.defines() && lastName == null) {
            return Optional.empty();
        }
        Builder builder = new Builder();
        for (WrittenStatement statement : statements.subList(0, statements.size() - 1)) {
            if (isElement(statement)) {
                String name = statement.name().orElseThrow();
                if (!name.equals(lastName)) {
                    builder.element(name, statement.expression(), name);
                }
            } else {
                builder.definitions.add(statement.text());
            }
        }
        Written lastBody = last.expression();
        builder.element(lastName != null ? lastName : "shown", lastBody,
                lastName != null ? lastName : lastBody.shape() instanceof WrittenStatement.Tuple ? " " : "");
        ObjectDefinition.GeometricConstruction construction = new ObjectDefinition.GeometricConstruction(builder.dimension,
                builder.elements);
        return Optional.of(new ObjectFiles.ReadObject("geometry", 1, source, construction, builder.parameters()));
    }

    /** Whether a statement names an element: {@code P = (1, 2)}, {@code S = segment(A, B)} — a name, no parameters. */
    private static boolean isElement(WrittenStatement statement) {
        return statement.defines() && statement.parameters().isEmpty() && statement.ranges().isEmpty()
                && isGeometry(statement.expression());
    }

    /** Whether an expression is a point `(a, b)` or a construction step `type(…)`. */
    private static boolean isGeometry(Written written) {
        return written.shape() instanceof WrittenStatement.Tuple
                || written.shape() instanceof WrittenStatement.Call call && STEP_WORDS.contains(call.name().toLowerCase(Locale.ROOT));
    }

    private static Set<String> wordsOf() {
        Set<String> words = new LinkedHashSet<>();
        for (GeometricElementType type : GeometricElementType.values()) {
            words.add(type.name().toLowerCase(Locale.ROOT));
        }
        return words;
    }

    /** The elements in the order they are built, the definitions their coordinates may call, and what they read. */
    private static final class Builder {

        final List<GeometricElement> elements = new ArrayList<>();
        final List<String> definitions = new ArrayList<>();
        final Map<String, GeometricElementType> known = new LinkedHashMap<>();
        int dimension = 2;
        int anonymous = 0;

        void element(String id, Written body, String label) {
            if (body.shape() instanceof WrittenStatement.Tuple tuple) {
                point(id, tuple, label);
                return;
            }
            if (!(body.shape() instanceof WrittenStatement.Call call)) {
                throw new IllegalArgumentException("'" + body.text() + "' is neither a point nor a construction step.");
            }
            GeometricElementType type = GeometricElementType.valueOf(call.name().toUpperCase(Locale.ROOT));
            List<String> references = new ArrayList<>();
            List<String> values = new ArrayList<>();
            for (Written argument : call.arguments()) {
                if (type == GeometricElementType.POINT) {
                    values.add(withDefinitions(argument.text()));
                } else if (known.containsKey(argument.text())) {
                    references.add(argument.text());
                } else if (argument.shape() instanceof WrittenStatement.Tuple tuple) {
                    String point = "point" + (++anonymous);
                    point(point, tuple, "");
                    references.add(point);
                } else if (isGeometry(argument)) {
                    // A step inside a step — rotation(polygon(…), C, θ): built first, as a helper nobody sees.
                    String inner = "shape" + (++anonymous);
                    element(inner, argument, "");
                    references.add(inner);
                } else {
                    values.add(withDefinitions(argument.text()));
                }
            }
            if (type == GeometricElementType.POINT) {
                dimension = Math.max(dimension, values.size());
            }
            elements.add(new GeometricElement(id, type, references, values, label));
            known.put(id, type);
        }

        void point(String id, WrittenStatement.Tuple tuple, String label) {
            List<Written> parts = tuple.components();
            if (parts.size() > 3) {
                throw new IllegalArgumentException("A point has two or three coordinates, not " + parts.size() + ".");
            }
            dimension = Math.max(dimension, parts.size());
            elements.add(new GeometricElement(id, GeometricElementType.POINT, List.of(),
                    parts.stream().map(part -> withDefinitions(part.text())).toList(), label));
            known.put(id, GeometricElementType.POINT);
        }

        /** A coordinate with the graph's own definitions in front of it, so it may call them: `f(x) = …; f(t)`. */
        String withDefinitions(String coordinate) {
            return definitions.isEmpty() ? coordinate : String.join("; ", definitions) + "; " + coordinate;
        }

        /** The names the coordinates read that are not constants, not elements and not defined here: the sliders. */
        List<ParameterDefinition> parameters() {
            FormulaLanguage language = FormulaLanguage.standard();
            Set<String> names = new LinkedHashSet<>();
            for (GeometricElement element : elements) {
                for (String coordinate : element.coordinates()) {
                    try {
                        names.addAll(language.parse(coordinate).variables());
                    } catch (FormulaException refused) {
                        throw new IllegalArgumentException(refused.getMessage(), refused);
                    }
                }
            }
            names.removeAll(known.keySet());
            return ObjectFiles.sliders(names);
        }
    }
}
