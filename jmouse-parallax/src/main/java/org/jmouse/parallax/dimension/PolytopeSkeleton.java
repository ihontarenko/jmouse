package org.jmouse.parallax.dimension;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.object.ObjectDefinition.Polytope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A polytope's vertices and edges, made from what its document writes — the polytope engine. No family is known
 * here: the vertices come from a rule the engine supplies and the edges from a condition on every two vertices, both
 * read as host calls on the expression's tree.
 *
 * <table>
 *   <tr><th>Vertex rule</th><th>Vertices</th></tr>
 *   <tr><td>{@code cartesian(a, b, …, count)}</td><td>every point of {@code count} coordinates, each one of the values,
 *       the first coordinate changing fastest — {@code cartesian(-1, 1, 4)} is the tesseract's 16</td></tr>
 *   <tr><td>{@code axis_points(radius, count)}</td><td>the points at ±radius on each of {@code count} axes, the
 *       positive one first — the cross polytope's</td></tr>
 *   <tr><td>{@code regular_simplex(count)}</td><td>{@code count + 1} points at equal distances, centred: the unit
 *       vectors of the space one dimension up, carried into this one</td></tr>
 * </table>
 *
 * <table>
 *   <tr><th>Edge measure</th><th>Of two vertices {@code first} and {@code second}</th></tr>
 *   <tr><td>{@code hamming(first, second)}</td><td>how many coordinates differ</td></tr>
 *   <tr><td>{@code dot(first, second)}</td><td>their dot product</td></tr>
 *   <tr><td>{@code distance(first, second)}</td><td>their Euclidean distance</td></tr>
 * </table>
 *
 * <p>Two vertices are joined when the condition holds: an equation holds when its sides agree to 1e-9, anything else
 * when it is not zero. Pairs are taken in order — the first index rising, then the second — so the edges come out in
 * the same order on every run. The rule and the condition may name {@code dimension}, the polytope's own.</p>
 */
public record PolytopeSkeleton(int dimension, List<double[]> vertices, List<int[]> edges) {

    public static final String CARTESIAN       = "cartesian";
    public static final String AXIS_POINTS     = "axis_points";
    public static final String REGULAR_SIMPLEX = "regular_simplex";
    public static final String HAMMING         = "hamming";
    public static final String DOT             = "dot";
    public static final String DISTANCE        = "distance";

    private static final String FIRST  = "first";
    private static final String SECOND = "second";

    private static final int MAXIMUM_VERTICES = 100_000;
    private static final long MAXIMUM_PAIRS   = 5_000_000;

    /** jMF with the engine's rules and measures as host calls. */
    public static final FormulaLanguage LANGUAGE = FormulaLanguage.standard()
            .withHostCalls(CARTESIAN, AXIS_POINTS, REGULAR_SIMPLEX, HAMMING, DOT, DISTANCE);

    public PolytopeSkeleton {
        vertices = List.copyOf(vertices);
        edges = List.copyOf(edges);
    }

    /**
     * @throws FormulaException when the rule is not one the engine supplies, its arguments are not numbers of
     *                          {@code dimension}, or the condition calls something it cannot measure
     */
    public static PolytopeSkeleton of(Polytope polytope) {
        int dimension = polytope.dimension();
        List<double[]> vertices = polytope.vertexRule() != null ? vertices(polytope.vertexRule(), dimension)
                : polytope.vertices().stream().map(vertex -> vertex.stream().mapToDouble(Double::doubleValue).toArray()).toList();
        for (double[] vertex : vertices) {
            if (vertex.length != dimension) {
                throw new FormulaException("Every vertex has " + dimension + " coordinates.", null, -1);
            }
        }
        List<int[]> edges;
        if (polytope.edgeCondition() != null) {
            edges = joined(polytope.edgeCondition(), vertices, dimension);
        } else if (!polytope.edges().isEmpty()) {
            edges = polytope.edges().stream().map(edge -> new int[] {edge.get(0), edge.get(1)}).toList();
            for (int[] edge : edges) {
                if (edge[0] < 0 || edge[1] < 0 || edge[0] >= vertices.size() || edge[1] >= vertices.size()) {
                    throw new FormulaException("An edge joins two listed vertices.", null, -1);
                }
            }
        } else {
            edges = shortestEdges(vertices);
        }
        return new PolytopeSkeleton(dimension, vertices, edges);
    }

    // ─── vertex rules ───────────────────────────────────────────────────────────────────────────────

    private static List<double[]> vertices(String rule, int dimension) {
        if (!(LANGUAGE.parse(rule) instanceof Formula.HostCall call)
                || !List.of(CARTESIAN, AXIS_POINTS, REGULAR_SIMPLEX).contains(call.name())) {
            throw new FormulaException("The vertices are made by " + CARTESIAN + "(values…, count), " + AXIS_POINTS
                    + "(radius, count) or " + REGULAR_SIMPLEX + "(count), not '" + rule + "'.", rule, -1);
        }
        double[] arguments = call.arguments().stream().mapToDouble(argument -> number(argument, dimension, rule)).toArray();
        return switch (call.name()) {
            case CARTESIAN -> {
                if (arguments.length < 2) {
                    throw new FormulaException(CARTESIAN + " takes the values a coordinate may take, then how many "
                            + "coordinates: cartesian(-1, 1, dimension).", rule, -1);
                }
                yield cartesian(Arrays.copyOf(arguments, arguments.length - 1), count(arguments[arguments.length - 1], rule));
            }
            case AXIS_POINTS -> {
                requireArguments(call, arguments, 2, "axis_points(radius, dimension)", rule);
                yield axisPoints(arguments[0], count(arguments[1], rule));
            }
            default -> {
                requireArguments(call, arguments, 1, "regular_simplex(dimension)", rule);
                yield regularSimplex(count(arguments[0], rule));
            }
        };
    }

    /** Every tuple of {@code count} values, the first coordinate changing fastest. */
    private static List<double[]> cartesian(double[] values, int count) {
        if (Math.pow(values.length, count) > MAXIMUM_VERTICES) {
            throw new FormulaException("cartesian makes at most " + MAXIMUM_VERTICES + " vertices.", null, -1);
        }
        int total = (int) Math.round(Math.pow(values.length, count));
        List<double[]> vertices = new ArrayList<>(total);
        for (int index = 0; index < total; index++) {
            double[] vertex = new double[count];
            int remainder = index;
            for (int axis = 0; axis < count; axis++) {
                vertex[axis] = values[remainder % values.length];
                remainder /= values.length;
            }
            vertices.add(vertex);
        }
        return vertices;
    }

    private static List<double[]> axisPoints(double radius, int count) {
        List<double[]> vertices = new ArrayList<>();
        for (int axis = 0; axis < count; axis++) {
            for (double sign : new double[] {1, -1}) {
                double[] vertex = new double[count];
                vertex[axis] = sign * radius;
                vertices.add(vertex);
            }
        }
        return vertices;
    }

    /** The n + 1 unit vectors of Rⁿ⁺¹ carried into Rⁿ — the last one onto the diagonal — and centred. */
    private static List<double[]> regularSimplex(int count) {
        List<double[]> vertices = new ArrayList<>();
        for (int index = 0; index <= count; index++) {
            double[] vertex = new double[count];
            if (index < count) {
                vertex[index] = 1;
            } else {
                Arrays.fill(vertex, (1 - Math.sqrt(count + 1)) / count);
            }
            vertices.add(vertex);
        }
        double[] centre = new double[count];
        for (double[] vertex : vertices) {
            for (int axis = 0; axis < count; axis++) {
                centre[axis] += vertex[axis] / vertices.size();
            }
        }
        for (double[] vertex : vertices) {
            for (int axis = 0; axis < count; axis++) {
                vertex[axis] -= centre[axis];
            }
        }
        return vertices;
    }

    // ─── edge conditions ────────────────────────────────────────────────────────────────────────────

    /** The pairs, first index rising and then the second, whose condition holds. */
    private static List<int[]> joined(String condition, List<double[]> vertices, int dimension) {
        if ((long) vertices.size() * (vertices.size() - 1) / 2 > MAXIMUM_PAIRS) {
            throw new FormulaException("An edge condition is tested on at most " + MAXIMUM_PAIRS + " pairs.", condition, -1);
        }
        Formula parsed = LANGUAGE.parse(condition);
        List<Measure> measures = new ArrayList<>();
        Formula bound = Substitution.replaceHostCalls(parsed, call -> {
            measures.add(measure(call, condition));
            return new Formula.Slot(measures.size() - 1, call.name());
        });
        RealFunction function = RealCompiler.compile(bound, Symbols.withSlots(measures.size(), List.of("dimension"), List.of()),
                condition);
        boolean equation = bound instanceof Formula.Equation;
        double[] buffer = new double[measures.size() + 1];
        buffer[measures.size()] = dimension;
        List<int[]> edges = new ArrayList<>();
        for (int first = 0; first < vertices.size(); first++) {
            for (int second = first + 1; second < vertices.size(); second++) {
                for (int index = 0; index < measures.size(); index++) {
                    buffer[index] = measures.get(index).of(vertices.get(first), vertices.get(second));
                }
                double value = function.evaluate(buffer);
                if (equation ? Math.abs(value) <= 1e-9 : value != 0 && !Double.isNaN(value)) {
                    edges.add(new int[] {first, second});
                }
            }
        }
        return edges;
    }

    /** One measure written in a condition, {@code hamming(first, second)}, with its two vertices in either order. */
    private record Measure(String name, boolean swapped) {

        double of(double[] first, double[] second) {
            double[] left = swapped ? second : first;
            double[] right = swapped ? first : second;
            return switch (name) {
                case HAMMING -> {
                    int differing = 0;
                    for (int axis = 0; axis < left.length; axis++) {
                        if (left[axis] != right[axis]) {
                            differing++;
                        }
                    }
                    yield differing;
                }
                case DOT -> {
                    double sum = 0;
                    for (int axis = 0; axis < left.length; axis++) {
                        sum += left[axis] * right[axis];
                    }
                    yield sum;
                }
                default -> HigherDimensionComputations.distance(left, right);
            };
        }
    }

    private static Measure measure(Formula.HostCall call, String condition) {
        if (!List.of(HAMMING, DOT, DISTANCE).contains(call.name()) || call.arguments().size() != 2
                || !(call.arguments().get(0) instanceof Formula.Variable(String left))
                || !(call.arguments().get(1) instanceof Formula.Variable(String right))
                || !(left.equals(FIRST) && right.equals(SECOND) || left.equals(SECOND) && right.equals(FIRST))) {
            throw new FormulaException("An edge condition measures the two vertices: " + HAMMING + "(first, second), "
                    + DOT + "(first, second) or " + DISTANCE + "(first, second), not " + call.name() + "("
                    + call.arguments().size() + " arguments).", condition, -1);
        }
        return new Measure(call.name(), left.equals(SECOND));
    }

    /** Without a rule for them, the edges are the pairs at the shortest distance any two vertices have. */
    private static List<int[]> shortestEdges(List<double[]> vertices) {
        double shortest = Double.POSITIVE_INFINITY;
        for (int first = 0; first < vertices.size(); first++) {
            for (int second = first + 1; second < vertices.size(); second++) {
                shortest = Math.min(shortest, HigherDimensionComputations.distance(vertices.get(first), vertices.get(second)));
            }
        }
        List<int[]> edges = new ArrayList<>();
        for (int first = 0; first < vertices.size(); first++) {
            for (int second = first + 1; second < vertices.size(); second++) {
                if (HigherDimensionComputations.distance(vertices.get(first), vertices.get(second)) < shortest * (1 + 1e-9)) {
                    edges.add(new int[] {first, second});
                }
            }
        }
        return edges;
    }

    // ─── arguments ──────────────────────────────────────────────────────────────────────────────────

    /** A rule's argument: a number, or an expression of the polytope's {@code dimension}. */
    private static double number(Formula argument, int dimension, String rule) {
        if (!argument.variables().stream().allMatch("dimension"::equals)) {
            throw new FormulaException("A vertex rule's arguments are numbers or 'dimension', not " + argument.variables()
                    + ".", rule, -1);
        }
        return RealCompiler.compile(argument, Symbols.of("dimension"), rule).evaluate(new double[] {dimension});
    }

    private static int count(double value, String rule) {
        if (value != Math.rint(value) || value < 1 || value > 8) {
            throw new FormulaException("A polytope has 1 to 8 coordinates, not " + value + ".", rule, -1);
        }
        return (int) value;
    }

    private static void requireArguments(Formula.HostCall call, double[] arguments, int expected, String written, String rule) {
        if (arguments.length != expected) {
            throw new FormulaException(call.name() + " is written " + written + ".", rule, -1);
        }
    }
}
