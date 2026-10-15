package org.jmouse.parallax.file;

import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A line of a graph that is geometry rather than a function: a point written as a tuple — {@code (cos t, sin t)}, the
 * notation a curve's components already have in jMX — or a step of a construction's own vocabulary —
 * {@code vector((0, 0), P)}, {@code segment(A, B)}, {@code circle(O, A)}. It is read as a small construction, so the
 * construction engine computes and draws it; nothing new is computed for the graph.
 *
 * <p>Labels say what is marked: a named element carries its name, a bare point a blank label (marked, no word), and
 * the helpers a call builds — the tail of a vector, the line itself when it is a shape — an empty one (not marked).</p>
 *
 * <p>The line comes as the graph writes it: its definitions first, then the line, joined by {@code ;}. A definition
 * {@code P = (…)} or {@code S = segment(…)} is an element the line may name; any other definition — a function, a
 * constant — is kept as jMF and written in front of every coordinate, so a coordinate may call it.</p>
 */
public final class GeometryLines {

    private static final Pattern DEFINITION = Pattern.compile("\\s*([A-Za-z][A-Za-z0-9_]*)\\s*=(?!=)\\s*(.+)", Pattern.DOTALL);
    private static final Pattern CALL = Pattern.compile("\\s*([A-Za-z_]+)\\s*\\((.*)\\)\\s*", Pattern.DOTALL);
    private static final Set<String> STEP_WORDS = wordsOf();

    private GeometryLines() {
    }

    /** The line as a construction, or empty when its last statement is not a point or a construction step. */
    public static Optional<ObjectFiles.ReadObject> read(String source) {
        List<String> statements = splitTopLevel(source, ';');
        if (statements.isEmpty()) {
            return Optional.empty();
        }
        String last = statements.getLast();
        Matcher named = DEFINITION.matcher(last);
        String lastName = named.matches() && isGeometry(named.group(2)) ? named.group(1) : null;
        String lastBody = lastName != null ? named.group(2) : last;
        if (!isGeometry(lastBody)) {
            return Optional.empty();
        }
        Builder builder = new Builder();
        for (String statement : statements.subList(0, statements.size() - 1)) {
            Matcher definition = DEFINITION.matcher(statement);
            if (definition.matches() && isGeometry(definition.group(2))) {
                if (!definition.group(1).equals(lastName)) {
                    builder.element(definition.group(1), definition.group(2), definition.group(1));
                }
            } else {
                builder.definitions.add(statement.strip());
            }
        }
        builder.element(lastName != null ? lastName : "shown", lastBody, lastName != null ? lastName : isTuple(lastBody.strip()) ? " " : "");
        int dimension = builder.dimension;
        ObjectDefinition.GeometricConstruction construction = new ObjectDefinition.GeometricConstruction(dimension, builder.elements);
        return Optional.of(new ObjectFiles.ReadObject("geometry", 1, source, construction, builder.parameters()));
    }

    /** Whether a statement is a point `(a, b)` or a construction step `type(…)`. */
    private static boolean isGeometry(String text) {
        String body = text.strip();
        if (isTuple(body)) {
            return true;
        }
        Matcher call = CALL.matcher(body);
        return call.matches() && STEP_WORDS.contains(call.group(1).toLowerCase(Locale.ROOT)) && closesAtEnd(body);
    }

    private static boolean isTuple(String body) {
        return body.startsWith("(") && closesAtEnd(body) && splitTopLevel(body.substring(1, body.length() - 1), ',').size() >= 2;
    }

    /** Whether the parenthesis opened first closes at the very end: `(a, b)` but not `(a) + (b)`. */
    private static boolean closesAtEnd(String body) {
        int depth = 0;
        for (int index = 0; index < body.length(); index++) {
            char character = body.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
                if (depth == 0 && index != body.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0 && body.endsWith(")");
    }

    private static List<String> splitTopLevel(String text, char separator) {
        return ObjectFiles.splitTopLevel(text, separator);
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

        void element(String id, String body, String label) {
            String written = body.strip();
            if (isTuple(written)) {
                point(id, written, label);
                return;
            }
            Matcher call = CALL.matcher(written);
            if (!call.matches()) {
                throw new IllegalArgumentException("'" + written + "' is neither a point nor a construction step.");
            }
            GeometricElementType type = GeometricElementType.valueOf(call.group(1).toUpperCase(Locale.ROOT));
            List<String> references = new ArrayList<>();
            List<String> values = new ArrayList<>();
            for (String argument : splitTopLevel(call.group(2), ',')) {
                if (type == GeometricElementType.POINT) {
                    values.add(withDefinitions(argument));
                } else if (known.containsKey(argument)) {
                    references.add(argument);
                } else if (isGeometry(argument) && !isTuple(argument)) {
                    // A step inside a step — rotation(polygon(…), C, θ): built first, as a helper nobody sees.
                    String inner = "shape" + (++anonymous);
                    element(inner, argument, "");
                    references.add(inner);
                } else if (isTuple(argument)) {
                    String point = "point" + (++anonymous);
                    point(point, argument, "");
                    references.add(point);
                } else {
                    values.add(withDefinitions(argument));
                }
            }
            if (type == GeometricElementType.POINT) {
                dimension = Math.max(dimension, values.size());
            }
            elements.add(new GeometricElement(id, type, references, values, label));
            known.put(id, type);
        }

        void point(String id, String tuple, String label) {
            List<String> parts = splitTopLevel(tuple.substring(1, tuple.length() - 1), ',');
            if (parts.size() > 3) {
                throw new IllegalArgumentException("A point has two or three coordinates, not " + parts.size() + ".");
            }
            dimension = Math.max(dimension, parts.size());
            elements.add(new GeometricElement(id, GeometricElementType.POINT, List.of(),
                    parts.stream().map(this::withDefinitions).toList(), label));
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
