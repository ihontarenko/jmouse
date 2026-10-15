package org.jmouse.parallax.program;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;
import org.jmouse.formula.Formula;
import org.jmouse.formula.ExactRationalEvaluator;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.Rational;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition.Program;
import org.jmouse.parallax.object.Randomness;
import org.jmouse.script.el.ProgramEvaluator;
import org.jmouse.script.el.node.ProgramNode;
import org.jmouse.script.el.Facades;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.budget.ScriptBudget;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * One run of a {@link Program}: its jMF definitions bound as functions the program can call, its parameters as the
 * program's variables, and the answer turned into the dataset it describes.
 *
 * <p>What a program may return, and what it is shown as:</p>
 * <ul>
 *   <li>a number, or a map of names to numbers → measurements;</li>
 *   <li>a list of numbers → a series over its index {@code n};</li>
 *   <li>a map of names to lists of numbers → a series whose first entry is the axis;</li>
 *   <li>a list of points — lists of two or three numbers → a point cloud;</li>
 *   <li>a map of names to lists of points → one cloud, each point coloured by the list it is in;</li>
 *   <li>a map of names to any of these → each one under its name, side by side.</li>
 * </ul>
 *
 * <p>The random functions of {@link Randomness} — {@code random_uniform()}, {@code random_normal()} and their kin,
 * every one but {@code white_noise()} — are the program's too, drawn from the run's seeded source, so a Monte Carlo
 * program is written in jMS and still gives the same answer for the same seed.</p>
 *
 * <p>⚠️ A program reaches nothing but its variables and its formulas: it runs with no facades, under a budget, so a
 * reader's code on the server can neither touch the host nor run forever.</p>
 */
public final class ProgramRun {

    private static final ProgramEvaluator EVALUATOR = new ProgramEvaluator();

    private ProgramRun() {
    }

    /** Refuses, at save, formulas that do not compile and a program that does not parse. */
    public static void check(CompiledObject object, Program program) {
        formulaFunctions(object, program);
        parse(program);
    }

    static Object run(CompiledObject object, ScriptBudget budget, SplittableRandom random) {
        Program program = (Program) object.definition();
        Map<String, Object> variables = new LinkedHashMap<>(object.parameterValues());
        program.integerInputs().forEach((name, value) -> variables.put(name, new BigInteger(value)));
        variables.putAll(object.exactIntegerValues());
        variables.putAll(formulaFunctions(object, program));
        for (Randomness.Draw draw : Randomness.Draw.values()) {
            if (draw != Randomness.Draw.WHITE_NOISE) {
                variables.put(draw.functionName(), new RandomFunction(draw, random));
            }
        }
        return EVALUATOR.run(parse(program), variables, Facades.none(), budget);
    }

    /** A random function of {@link Randomness} seen through jME's function contract, drawing from the run's source. */
    private record RandomFunction(Randomness.Draw draw, SplittableRandom random) implements Function {

        @Override
        public Object execute(Arguments arguments, EvaluationContext context) {
            double[] values = new double[arguments.size()];
            for (int index = 0; index < values.length; index++) {
                values[index] = context.getConversion().convert(arguments.get(index), Double.class);
            }
            return draw.draw(random, values);
        }

        @Override
        public String getName() {
            return draw.functionName();
        }
    }

    /** The answer of a program as the dataset it describes, or a refusal that says which shapes there are. */
    static Dataset datasetOf(Object answer) {
        return dataset(normalized(answer));
    }

    private static Dataset dataset(Object value) {
        if (value instanceof Number number) {
            return new Dataset.Measurements(Map.of("result", number.doubleValue()), Map.of());
        }
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            if (map.size() == 1 && map.containsKey("bitmap")) {
                return bitmap(map.get("bitmap"));
            }
            if (map.values().stream().allMatch(Number.class::isInstance)) {
                Map<String, Double> values = new LinkedHashMap<>();
                map.forEach((key, entry) -> values.put(String.valueOf(key), ((Number) entry).doubleValue()));
                return new Dataset.Measurements(values, Map.of());
            }
            if (map.values().stream().allMatch(entry -> entry instanceof List<?> list && !list.isEmpty()
                    && list.stream().allMatch(Number.class::isInstance))) {
                return columns(map);
            }
            if (map.values().stream().allMatch(entry -> entry instanceof List<?> list && !list.isEmpty()
                    && list.stream().allMatch(ProgramRun::isPoint))) {
                return groupedPoints(map);
            }
            Map<String, Dataset> parts = new LinkedHashMap<>();
            map.forEach((key, entry) -> parts.put(String.valueOf(key), dataset(entry)));
            return new Dataset.Composite(parts);
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            if (list.stream().allMatch(Number.class::isInstance)) {
                float[] index = new float[list.size()];
                for (int position = 0; position < index.length; position++) {
                    index[position] = position;
                }
                return new Dataset.TimeSeries("n", index, List.of("value"), List.of(floats(list)));
            }
            if (list.stream().allMatch(ProgramRun::isPoint)) {
                return points(list);
            }
        }
        throw new IllegalArgumentException("A program returns a number, a list of numbers, a list of points [x, y] or "
                + "[x, y, z], or a map of names to any of these; this one returned " + describe(value) + ".");
    }

    private static Dataset.BinaryGrid bitmap(Object raw) {
        if (!(raw instanceof List<?> rows) || rows.isEmpty() || rows.size() > 1_024
                || !(rows.getFirst() instanceof List<?> first) || first.isEmpty() || first.size() > 1_024
                || (long) rows.size() * first.size() > 1_000_000) {
            throw new IllegalArgumentException("A bitmap needs 1–1024 rows and columns, at most one million pixels.");
        }
        int columns = first.size();
        int[] pixels = new int[rows.size() * columns];
        for (int row = 0; row < rows.size(); row++) {
            if (!(rows.get(row) instanceof List<?> values) || values.size() != columns) {
                throw new IllegalArgumentException("Every bitmap row must have the same width.");
            }
            for (int column = 0; column < columns; column++) {
                Object entry = values.get(column);
                if (!(entry instanceof Number number) || !(number.doubleValue() == 0 || number.doubleValue() == 1)) {
                    throw new IllegalArgumentException("A bitmap pixel must be 0 or 1.");
                }
                pixels[row * columns + column] = number.intValue();
            }
        }
        return new Dataset.BinaryGrid(columns, rows.size(), pixels);
    }

    private static ProgramNode parse(Program program) {
        try {
            return EVALUATOR.parse(program.program(), "program");
        } catch (ScriptParseException refused) {
            throw new FormulaException("The program does not read: " + refused.getMessage(), null, -1);
        }
    }

    /** Every jMF definition as a function the program calls by its name, bound to the object's parameters. */
    private static Map<String, Function> formulaFunctions(CompiledObject object, Program program) {
        Map<String, Function> functions = new LinkedHashMap<>();
        if (program.formulas().isBlank()) {
            return functions;
        }
        for (Formula.Definition definition : FormulaLanguage.standard().definitions(program.formulas())) {
            BoundFunction bound = object.function(definition.body(), definition.parameters());
            functions.put(definition.name(), new FormulaFunction(definition.name(), definition.parameters(),
                    definition.body(), bound, object.parameterValues()));
        }
        return functions;
    }

    /** A jMF definition seen through jME's function contract. */
    private record FormulaFunction(String name, List<String> parameters, Formula body, BoundFunction bound,
                                   Map<String, Double> objectParameters) implements Function {

        @Override
        public Object execute(Arguments arguments, EvaluationContext context) {
            if (arguments.size() != parameters.size()) {
                throw new IllegalArgumentException("'%s' takes %d argument(s), not %d".formatted(name, parameters.size(), arguments.size()));
            }
            if (containsBigInteger(arguments)) {
                Map<String, Rational> values = new LinkedHashMap<>();
                objectParameters.forEach((key, value) -> {
                    if (Double.isFinite(value)) {
                        values.put(key, Rational.ofDecimal(value));
                    }
                });
                for (int index = 0; index < parameters.size(); index++) {
                    Object argument = arguments.get(index);
                    if (!(argument instanceof Number number)) {
                        throw new IllegalArgumentException("An exact formula takes numbers only.");
                    }
                    values.put(parameters.get(index), number instanceof BigInteger integer ? Rational.of(integer)
                            : argument instanceof Long || argument instanceof Integer || argument instanceof Short || argument instanceof Byte
                            ? Rational.of(number.longValue()) : Rational.of(exactInteger(number)));
                }
                Rational result = ExactRationalEvaluator.evaluate(body, values);
                return result.isInteger() ? result.numerator() : result.doubleValue();
            }
            double[] values = new double[parameters.size()];
            for (int index = 0; index < parameters.size(); index++) {
                values[index] = context.getConversion().convert(arguments.get(index), Double.class);
            }
            return bound.at(values);
        }

        private static boolean containsBigInteger(Arguments arguments) {
            for (int index = 0; index < arguments.size(); index++) {
                if (arguments.get(index) instanceof BigInteger) {
                    return true;
                }
            }
            return false;
        }

        private static BigInteger exactInteger(Number number) {
            double value = number.doubleValue();
            if (!Double.isFinite(value) || value != Math.rint(value) || Math.abs(value) > 9_007_199_254_740_991d) {
                throw new IllegalArgumentException("An exact formula takes exact integer arguments only.");
            }
            return BigInteger.valueOf((long) value);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static Dataset columns(Map<?, ?> map) {
        List<String> names = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        map.forEach((key, column) -> {
            names.add(String.valueOf(key));
            values.add(floats((List<?>) column));
        });
        int length = values.getFirst().length;
        if (values.stream().anyMatch(column -> column.length != length)) {
            throw new IllegalArgumentException("Every column a program returns has the length of its first, the axis.");
        }
        return new Dataset.TimeSeries(names.getFirst(), values.getFirst(), names.subList(1, names.size()), values.subList(1, values.size()));
    }

    /** Lists of points under names: one cloud whose value is the position of a point's list, its name the lists'. */
    private static Dataset groupedPoints(Map<?, ?> groups) {
        List<?> first = (List<?>) groups.values().iterator().next();
        int dimension = ((List<?>) first.getFirst()).size();
        List<Float> coordinates = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        int group = 0;
        for (Object entry : groups.values()) {
            for (Object point : (List<?>) entry) {
                List<?> coordinatesOfPoint = (List<?>) point;
                if (coordinatesOfPoint.size() != dimension) {
                    throw new IllegalArgumentException("Every point a program returns has the dimension of its first.");
                }
                coordinatesOfPoint.forEach(coordinate -> coordinates.add(((Number) coordinate).floatValue()));
                values.add((float) group);
            }
            group++;
        }
        float[] flat = new float[coordinates.size()];
        for (int index = 0; index < flat.length; index++) {
            flat[index] = coordinates.get(index);
        }
        float[] groupOf = new float[values.size()];
        for (int index = 0; index < groupOf.length; index++) {
            groupOf[index] = values.get(index);
        }
        String names = String.join(" · ", groups.keySet().stream().map(String::valueOf).toList());
        return new Dataset.PointCloud(dimension, flat, groupOf, names);
    }

    private static Dataset points(List<?> list) {
        int dimension = ((List<?>) list.getFirst()).size();
        float[] coordinates = new float[list.size() * dimension];
        for (int point = 0; point < list.size(); point++) {
            List<?> coordinatesOfPoint = (List<?>) list.get(point);
            if (coordinatesOfPoint.size() != dimension) {
                throw new IllegalArgumentException("Every point a program returns has the dimension of its first.");
            }
            for (int axis = 0; axis < dimension; axis++) {
                coordinates[point * dimension + axis] = ((Number) coordinatesOfPoint.get(axis)).floatValue();
            }
        }
        return new Dataset.PointCloud(dimension, coordinates, null, null);
    }

    private static boolean isPoint(Object value) {
        return value instanceof List<?> point && (point.size() == 2 || point.size() == 3) && point.stream().allMatch(Number.class::isInstance);
    }

    private static float[] floats(List<?> list) {
        float[] values = new float[list.size()];
        for (int index = 0; index < values.length; index++) {
            if (!(list.get(index) instanceof Number number)) {
                throw new IllegalArgumentException("A column a program returns holds numbers only.");
            }
            values[index] = number.floatValue();
        }
        return values;
    }

    /** Arrays (what a list literal evaluates to) become lists, recursively, so one set of rules reads every answer. */
    private static Object normalized(Object value) {
        if (value != null && value.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            for (int index = 0; index < Array.getLength(value); index++) {
                list.add(normalized(Array.get(value, index)));
            }
            return list;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(ProgramRun::normalized).toList();
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, entry) -> copy.put(key, normalized(entry)));
            return copy;
        }
        return value;
    }

    private static String describe(Object value) {
        return value == null ? "nothing" : value instanceof List<?> list ? "a list of " + list.size() : value.getClass().getSimpleName();
    }
}
