package org.jmouse.parallax.file;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reading mathematical objects out of files. A .jmf file holds one formula per line ('#' comments, 'name:' labels);
 * each formula becomes the object its variables say it is — x alone a curve y = f(x), x and y a surface
 * z = f(x, y), x, y and z an implicit surface, four of them a hypersurface, z a complex mapping, n a sequence —
 * and every other name it mentions becomes a parameter. A CSV file of numbers becomes a point set.
 */
public final class ObjectFiles {

    private static final Pattern LABELLED = Pattern.compile("^\\s*([\\p{L}][\\w\\p{L}-]*)\\s*:\\s*(.+)$");
    private static final Set<String> CONSTANT_NAMES = Set.of("pi", "e", "tau", "phi", "i");

    /** Whether a name is one of the language's constants, which is never a slider. */
    public static boolean isConstantName(String name) {
        return CONSTANT_NAMES.contains(name);
    }

    private ObjectFiles() {
    }

    /** A slider for every name, in order, constants left out: what a formula read from text is given. */
    static List<ParameterDefinition> sliders(Set<String> names) {
        List<ParameterDefinition> parameters = new ArrayList<>();
        for (String name : names) {
            if (!isConstantName(name)) {
                parameters.add(new ParameterDefinition(name, name, 1, -10, 10, 0.01, false));
            }
        }
        return parameters;
    }

    /** One object read out of a file, with the line it came from and the parameters its formula needs. */
    public record ReadObject(String name, int line, String source, ObjectDefinition definition,
                             List<ParameterDefinition> parameters) {
    }

    /** One line that could not be read, and why. */
    public record Refusal(int line, String text, String reason) {
    }

    public record Reading(List<ReadObject> objects, List<Refusal> refusals) {
    }

    public static Reading readFormulaFile(String content) {
        List<ReadObject> objects = new ArrayList<>();
        List<Refusal> refusals = new ArrayList<>();
        String[] lines = content.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher labelled = LABELLED.matcher(line);
            String name = labelled.matches() ? labelled.group(1) : "formula-" + (index + 1);
            String source = labelled.matches() ? labelled.group(2).strip() : line;
            try {
                objects.add(infer(name, index + 1, source));
            } catch (FormulaException | IllegalArgumentException refused) {
                refusals.add(new Refusal(index + 1, lines[index], refused.getMessage()));
            }
        }
        return new Reading(objects, refusals);
    }

    /**
     * What a single formula is, read from its shape and the variables it mentions: {@code y = f(x)} is a curve and
     * {@code z = f(x, y)} a surface, any other equation in x or y is the curve where it holds ({@code x = 2} a vertical line), a condition in x and
     * y is the region where it holds; otherwise the variables decide, and a formula that mentions none is a value.
     */
    public static ReadObject infer(String name, int line, String source) {
        FormulaLanguage language = FormulaLanguage.standard();
        Formula formula = language.parse(source);
        Set<String> mentioned = formula.variables();
        boolean planar = !mentioned.contains("z") && !mentioned.contains("w") && !formula.mentionsImaginaryUnit();
        ObjectDefinition definition;
        List<String> variables;
        Optional<String> curve = planar ? language.solvedFor(source, "y") : Optional.empty();
        Optional<String> surface = mentioned.contains("z") && !mentioned.contains("w") ? language.solvedFor(source, "z") : Optional.empty();
        if (curve.isPresent()) {
            variables = List.of("x");
            definition = new ObjectDefinition.ExplicitCurve(curve.get(), Interval.of(-5, 5), variables);
        } else if (surface.isPresent()) {
            variables = List.of("x", "y");
            definition = new ObjectDefinition.ExplicitSurface(surface.get(), Interval.of(-3, 3), Interval.of(-3, 3), variables);
        } else if (planar && formula instanceof Formula.Equation && (mentioned.contains("x") || mentioned.contains("y"))) {
            // `x = 2` is the vertical line where it holds, not a function of x: only `y = …` is solved for.
            variables = List.of("x", "y");
            definition = new ObjectDefinition.ImplicitCurve(source, Interval.of(-5, 5), Interval.of(-5, 5), variables);
        } else if (planar && isCondition(formula) && (mentioned.contains("x") || mentioned.contains("y"))) {
            variables = List.of("x", "y");
            definition = new ObjectDefinition.Region(source, variables, List.of("-5", "-5"), List.of("5", "5"), false, Map.of());
        } else if (formula.mentionsImaginaryUnit() || (mentioned.contains("z") && !mentioned.contains("x") && !mentioned.contains("y"))) {
            variables = List.of("z");
            definition = new ObjectDefinition.ComplexMapping(source, Interval.of(-2, 2), Interval.of(-2, 2), variables);
        } else if (mentioned.contains("w")) {
            definition = new ObjectDefinition.ImplicitHypersurface(source, Interval.of(-2, 2), Interval.of(-2, 2),
                    Interval.of(-2, 2), Interval.of(-2, 2));
            variables = List.of("x", "y", "z", "w");
        } else if (mentioned.contains("z")) {
            variables = List.of("x", "y", "z");
            definition = new ObjectDefinition.ImplicitSurface(source, Interval.of(-2, 2), Interval.of(-2, 2), Interval.of(-2, 2),
                    variables);
        } else if (mentioned.contains("y")) {
            variables = List.of("x", "y");
            definition = new ObjectDefinition.ExplicitSurface(source, Interval.of(-3, 3), Interval.of(-3, 3), variables);
        } else if (mentioned.contains("x")) {
            variables = List.of("x");
            definition = new ObjectDefinition.ExplicitCurve(source, Interval.of(-5, 5), variables);
        } else if (mentioned.contains("n")) {
            variables = List.of("n");
            definition = new ObjectDefinition.Sequence(source, 1, 100, variables);
        } else {
            // None of the variables: one number, its other names the sliders it depends on.
            variables = List.of();
            definition = new ObjectDefinition.Value("value", source);
        }
        // A solved equation names its left side nowhere else: `y = a x` has the parameter a, not y.
        Set<String> parameterNames = new LinkedHashSet<>(curve.or(() -> surface).map(solved -> language.parse(solved).variables()).orElse(mentioned));
        parameterNames.removeAll(variables);
        return new ReadObject(name, line, source, definition, sliders(parameterNames));
    }

    /** A formula that is true or false at a point — a comparison, a conjunction of them, a negation. */
    private static boolean isCondition(Formula formula) {
        return formula instanceof Formula.Comparison || formula instanceof Formula.Logical || formula instanceof Formula.Not;
    }

    /** A CSV of numbers, one point per row; a header row of names is skipped. */
    public static ObjectDefinition.PointSet readPointTable(String content) {
        List<List<Double>> points = new ArrayList<>();
        int dimension = -1;
        for (String raw : content.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split("[,;\\t]");
            List<Double> point = new ArrayList<>(cells.length);
            boolean numeric = true;
            for (String cell : cells) {
                double coordinate;
                try {
                    coordinate = Double.parseDouble(cell.strip());
                } catch (NumberFormatException header) {
                    numeric = false;
                    break;
                }
                if (!Double.isFinite(coordinate)) {
                    throw new IllegalArgumentException("The row '" + line + "' has a coordinate that is not a finite number.");
                }
                point.add(coordinate);
            }
            if (!numeric) {
                if (points.isEmpty()) {
                    continue;
                }
                throw new IllegalArgumentException("The row '" + line + "' is not numbers.");
            }
            if (dimension < 0) {
                dimension = point.size();
            } else if (point.size() != dimension) {
                throw new IllegalArgumentException("Every row has " + dimension + " numbers; '" + line + "' does not.");
            }
            points.add(point);
        }
        if (points.isEmpty()) {
            throw new IllegalArgumentException("The table has no rows of numbers.");
        }
        return new ObjectDefinition.PointSet(dimension, points);
    }
}
