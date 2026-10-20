package org.jmouse.parallax.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a mathematical object is, as data. The shape is closed — each kind is one record — so every consumer
 * (the compiler, the computations, the evaluation checks) handles the set exhaustively and a new kind is a
 * compile error everywhere it has not been thought about yet.
 *
 * <p>Expressions are kept as the text a person wrote; {@link DefinitionCompiler} turns them into compiled
 * functions over the kind's variables and the object's parameters, and refuses a definition that does not compile.
 *
 * <h2>Variables are named by the author</h2>
 *
 * <p>A curve may run over {@code t} or {@code θ}, a surface over {@code u, v} or {@code a1, a2}, a sequence over
 * {@code k}: a definition carries the names its expressions are written in ({@code variables}), and every computation
 * binds those names. The intervals keep the name of the axis of the picture they span — an explicit curve's input
 * runs along {@code x} whatever it is called — so a variable's name is never read off an interval's.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ObjectDefinition.ExplicitCurve.class, name = "EXPLICIT_CURVE"),
        @JsonSubTypes.Type(value = ObjectDefinition.ExplicitSurface.class, name = "EXPLICIT_SURFACE"),
        @JsonSubTypes.Type(value = ObjectDefinition.ImplicitCurve.class, name = "IMPLICIT_CURVE"),
        @JsonSubTypes.Type(value = ObjectDefinition.ImplicitSurface.class, name = "IMPLICIT_SURFACE"),
        @JsonSubTypes.Type(value = ObjectDefinition.ImplicitHypersurface.class, name = "IMPLICIT_HYPERSURFACE"),
        @JsonSubTypes.Type(value = ObjectDefinition.ParametricCurve.class, name = "PARAMETRIC_CURVE"),
        @JsonSubTypes.Type(value = ObjectDefinition.ParametricSurface.class, name = "PARAMETRIC_SURFACE"),
        @JsonSubTypes.Type(value = ObjectDefinition.VectorField.class, name = "VECTOR_FIELD"),
        @JsonSubTypes.Type(value = ObjectDefinition.DifferentialSystem.class, name = "DIFFERENTIAL_SYSTEM"),
        @JsonSubTypes.Type(value = ObjectDefinition.DiscreteMap.class, name = "DISCRETE_MAP"),
        @JsonSubTypes.Type(value = ObjectDefinition.ComplexMapping.class, name = "COMPLEX_MAPPING"),
        @JsonSubTypes.Type(value = ObjectDefinition.EscapeTimeFractal.class, name = "ESCAPE_TIME_FRACTAL"),
        @JsonSubTypes.Type(value = ObjectDefinition.NewtonFractal.class, name = "NEWTON_FRACTAL"),
        @JsonSubTypes.Type(value = ObjectDefinition.IteratedFunctionSystem.class, name = "ITERATED_FUNCTION_SYSTEM"),
        @JsonSubTypes.Type(value = ObjectDefinition.LindenmayerSystem.class, name = "LINDENMAYER_SYSTEM"),
        @JsonSubTypes.Type(value = ObjectDefinition.MatrixObject.class, name = "MATRIX"),
        @JsonSubTypes.Type(value = ObjectDefinition.AffineTransformation.class, name = "AFFINE_TRANSFORMATION"),
        @JsonSubTypes.Type(value = ObjectDefinition.NonlinearTransformation.class, name = "NONLINEAR_TRANSFORMATION"),
        @JsonSubTypes.Type(value = ObjectDefinition.PointSet.class, name = "POINT_SET"),
        @JsonSubTypes.Type(value = ObjectDefinition.GeometricConstruction.class, name = "GEOMETRIC_CONSTRUCTION"),
        @JsonSubTypes.Type(value = ObjectDefinition.Polytope.class, name = "POLYTOPE"),
        @JsonSubTypes.Type(value = ObjectDefinition.ProbabilityDistribution.class, name = "PROBABILITY_DISTRIBUTION"),
        @JsonSubTypes.Type(value = ObjectDefinition.Region.class, name = "REGION"),
        @JsonSubTypes.Type(value = ObjectDefinition.Sequence.class, name = "SEQUENCE"),
        @JsonSubTypes.Type(value = ObjectDefinition.Value.class, name = "VALUE"),
        @JsonSubTypes.Type(value = ObjectDefinition.Circuit.class, name = "CIRCUIT"),
        @JsonSubTypes.Type(value = ObjectDefinition.Program.class, name = "PROGRAM"),
        @JsonSubTypes.Type(value = ObjectDefinition.Component.class, name = "COMPONENT"),
        @JsonSubTypes.Type(value = ObjectDefinition.Symbol.class, name = "SYMBOL"),
        @JsonSubTypes.Type(value = ObjectDefinition.Graph.class, name = "GRAPH"),
        @JsonSubTypes.Type(value = ObjectDefinition.Family.class, name = "FAMILY")
})
public sealed interface ObjectDefinition {

    /** What a bare primitive's terminals are called in what it reports: its "+" and its "−". */
    String PRIMITIVE_POSITIVE = "+";
    String PRIMITIVE_NEGATIVE = "−";


    @JsonIgnore
    ObjectKind objectKind();

    /** The dimension of the space the object lives in; 4 and above are shown through projection and slicing. */
    @JsonIgnore
    int ambientDimension();

    /**
     * The names a definition's expressions are written in: the author's, or the conventional ones when none were
     * given (a definition read from JSON written before variables were named).
     */
    private static List<String> named(List<String> written, List<String> conventional, String what) {
        if (written == null || written.isEmpty()) {
            return conventional;
        }
        List<String> names = List.copyOf(written);
        if (names.size() != conventional.size() || Set.copyOf(names).size() != names.size()
                || names.stream().anyMatch(name -> !name.matches("[A-Za-z][A-Za-z0-9_]*"))) {
            throw new IllegalArgumentException(what + " is written in " + conventional.size() + " distinct variable(s), "
                    + "each a name such as x or theta, not " + names + ".");
        }
        return names;
    }

    /** y = f(x), over an input of any name: the interval is the horizontal axis it runs along. */
    record ExplicitCurve(String expression, Interval x, List<String> variables) implements ObjectDefinition {
        public ExplicitCurve {
            variables = named(variables, List.of("x"), "An explicit curve");
        }

        public ObjectKind objectKind() {
            return ObjectKind.EXPLICIT_CURVE;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /** z = f(x, y), over two inputs of any names: the intervals are the horizontal axes they run along. */
    record ExplicitSurface(String expression, Interval x, Interval y, List<String> variables) implements ObjectDefinition {
        public ExplicitSurface {
            variables = named(variables, List.of("x", "y"), "An explicit surface");
        }

        public ObjectKind objectKind() {
            return ObjectKind.EXPLICIT_SURFACE;
        }

        public int ambientDimension() {
            return 3;
        }
    }

    /** f(x, y) = 0, the two inputs named as the author named them. */
    record ImplicitCurve(String expression, Interval x, Interval y, List<String> variables) implements ObjectDefinition {
        public ImplicitCurve {
            variables = named(variables, List.of("x", "y"), "An implicit curve");
        }

        public ObjectKind objectKind() {
            return ObjectKind.IMPLICIT_CURVE;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /** f(x, y, z) = 0: a sphere, a torus by its equation, a gyroid. */
    record ImplicitSurface(String expression, Interval x, Interval y, Interval z, List<String> variables)
            implements ObjectDefinition {
        public ImplicitSurface {
            variables = named(variables, List.of("x", "y", "z"), "An implicit surface");
        }

        public ObjectKind objectKind() {
            return ObjectKind.IMPLICIT_SURFACE;
        }

        public int ambientDimension() {
            return 3;
        }
    }

    /** f(x, y, z, w) = 0: the hypersphere, seen through its 3D slices at w = const. */
    record ImplicitHypersurface(String expression, Interval x, Interval y, Interval z, Interval w)
            implements ObjectDefinition {
        public ObjectKind objectKind() {
            return ObjectKind.IMPLICIT_HYPERSURFACE;
        }

        public int ambientDimension() {
            return 4;
        }
    }

    /** t ↦ (x(t), y(t)[, z(t)]): helix, cycloid, Lissajous, a knot (closed); the parameter may be named anything. */
    record ParametricCurve(List<String> components, Interval t, boolean closed, List<String> variables)
            implements ObjectDefinition {
        public ParametricCurve {
            components = List.copyOf(components);
            variables = named(variables, List.of("t"), "A parametric curve");
            if (components.size() < 2 || components.size() > 4) {
                throw new IllegalArgumentException("A parametric curve has 2, 3 or 4 components.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.PARAMETRIC_CURVE;
        }

        public int ambientDimension() {
            return components.size();
        }
    }

    /**
     * (u, v) ↦ (x, y, z[, w]): torus, Möbius strip, Klein bottle, Enneper; with 4 components a surface in R⁴
     * (the Clifford torus). `gluing` says how the parameter square's edges are identified — the topology lab's
     * seam, orientation and cut/glue animation read it.
     */
    record ParametricSurface(List<String> components, Interval u, Interval v, SurfaceGluing gluing, List<String> variables)
            implements ObjectDefinition {
        public ParametricSurface {
            components = List.copyOf(components);
            variables = named(variables, List.of("u", "v"), "A parametric surface");
            if (components.size() < 3 || components.size() > 4) {
                throw new IllegalArgumentException("A parametric surface has 3 or 4 components.");
            }
            if (gluing == null) {
                gluing = SurfaceGluing.NONE;
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.PARAMETRIC_SURFACE;
        }

        public int ambientDimension() {
            return components.size();
        }
    }

    /** F(x, y[, z]): one component per axis, over a box. */
    record VectorField(List<String> components, List<Interval> bounds) implements ObjectDefinition {
        public VectorField {
            components = List.copyOf(components);
            bounds = List.copyOf(bounds);
            if (components.size() < 2 || components.size() > 3 || bounds.size() != components.size()) {
                throw new IllegalArgumentException("A vector field has 2 or 3 components and one bound per axis.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.VECTOR_FIELD;
        }

        public int ambientDimension() {
            return components.size();
        }
    }

    /**
     * dX/dt = F(X, t): one equation per state variable. Higher-order systems are written first-order (the
     * pendulum is θ' = ω, ω' = -sin θ). `bounds` frame the phase space; `time` is the default integration span.
     *
     * <p>An equation may call {@code white_noise()}: the system is then stochastic, integrated by Euler–Maruyama as
     * sample paths from its {@code starts}, and the deterministic views (trajectory, portrait, equilibria) do not
     * apply to it — see {@link Randomness}.</p>
     *
     * @param starts where each variable starts, as written: an expression of the parameters
     */
    record DifferentialSystem(List<String> variables, List<String> equations, Interval time, List<Interval> bounds,
                              List<String> starts, FlowImage image) implements ObjectDefinition {

        public DifferentialSystem(List<String> variables, List<String> equations, Interval time, List<Interval> bounds,
                                  List<String> starts) {
            this(variables, equations, time, bounds, starts, null);
        }

        public DifferentialSystem {
            variables = List.copyOf(variables);
            equations = List.copyOf(equations);
            bounds = bounds == null ? List.of() : List.copyOf(bounds);
            starts = startsOf(starts, variables.size());
            if (variables.isEmpty() || variables.size() > 8 || variables.size() != equations.size()) {
                throw new IllegalArgumentException("A system has 1–8 variables and one equation for each.");
            }
            if (!bounds.isEmpty() && bounds.size() != variables.size()) {
                throw new IllegalArgumentException("Phase-space bounds name one interval per variable.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.DIFFERENTIAL_SYSTEM;
        }

        public int ambientDimension() {
            return Math.max(1, variables.size());
        }
    }

    /**
     * X ↦ G(X): the logistic map, the Hénon map, a random walk. Iterated rather than integrated.
     *
     * <p>An update may call a {@code random_…} function: the map is then stochastic, run as sample paths from its
     * {@code starts}, and the deterministic views do not apply to it — see {@link Randomness}.</p>
     *
     * @param starts     where each variable starts, as written: an expression of the parameters
     * @param stepValues values computed once per step, before the updates, which every update may read — so two
     *                   updates that read one {@code random_…} draw see the same number
     */
    record DiscreteMap(List<String> variables, List<String> updates, List<Interval> bounds, List<String> starts,
                       List<StepValue> stepValues) implements ObjectDefinition {
        public DiscreteMap {
            variables = List.copyOf(variables);
            updates = List.copyOf(updates);
            bounds = bounds == null ? List.of() : List.copyOf(bounds);
            starts = startsOf(starts, variables.size());
            stepValues = stepValues == null ? List.of() : List.copyOf(stepValues);
            if (variables.isEmpty() || variables.size() > 4 || variables.size() != updates.size()) {
                throw new IllegalArgumentException("A map has 1–4 variables and one update for each.");
            }
            Set<String> names = new HashSet<>(variables);
            for (StepValue value : stepValues) {
                if (!names.add(value.name())) {
                    throw new IllegalArgumentException("'" + value.name() + "' is named twice in the state and the step's values.");
                }
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.DISCRETE_MAP;
        }

        public int ambientDimension() {
            return variables.size();
        }
    }

    /** {@code local axis = random_integer(0, 1)}: one value of a step, computed before the updates read it. */
    record StepValue(String name, String expression) {
        public StepValue {
            if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*") || expression == null || expression.isBlank()) {
                throw new IllegalArgumentException("A step's value is 'local name = expression'.");
            }
        }
    }

    /** One start per variable; none written means every variable starts at 0. */
    private static List<String> startsOf(List<String> starts, int variables) {
        if (starts == null || starts.isEmpty()) {
            return Collections.nCopies(variables, "0");
        }
        if (starts.size() != variables || starts.stream().anyMatch(start -> start == null || start.isBlank())) {
            throw new IllegalArgumentException("Every state variable says where it starts.");
        }
        return List.copyOf(starts);
    }

    /** w = f(z) over a rectangle of the complex plane, the complex input named as the author named it. */
    record ComplexMapping(String expression, Interval real, Interval imaginary, List<String> variables)
            implements ObjectDefinition {
        public ComplexMapping {
            variables = named(variables, List.of("z"), "A complex mapping");
        }

        public ObjectKind objectKind() {
            return ObjectKind.COMPLEX_MAPPING;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /**
     * A complex state stepped from every point of a rectangle of the plane, counted until it leaves the bailout radius:
     * the escape-time engine an {@code iteration} whose output is {@code escape_time(…)} is computed by.
     *
     * <p>Nothing here names a fractal. Mandelbrot's set is {@code state} z starting at {@code 0} over the {@code input}
     * c; Julia's is the state starting at the point itself — the input <em>is</em> the state — with c a parameter of
     * the iteration. Which picture it is follows from the document, not from a family word.</p>
     *
     * @param state     the variable the step advances: {@code z} in {@code z_next = z^2 + c}
     * @param start     the state's first value, an expression of the input; the input's own name when they are one
     * @param iteration the step, {@code z^2 + c}, with any local definitions in front of it
     * @param input     the complex variable that ranges over the rectangle
     */
    record EscapeTimeFractal(String state, String start, String iteration, String input, double bailout,
                             int maximumIterations, Interval real, Interval imaginary) implements ObjectDefinition {
        public EscapeTimeFractal {
            if (state == null || input == null || iteration == null || iteration.isBlank()) {
                throw new IllegalArgumentException("An escape-time iteration names its state, its input and its step.");
            }
            if (start == null || start.isBlank()) {
                if (!state.equals(input)) {
                    throw new IllegalArgumentException("The state '" + state + "' starts somewhere: 'state { "
                            + state + " = 0 continuous }', or it is the input itself.");
                }
                start = input;
            }
            if (!(bailout > 0)) {
                bailout = 2;
            }
            if (maximumIterations < 1 || maximumIterations > 100_000) {
                throw new IllegalArgumentException("Iterations are between 1 and 100000.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.ESCAPE_TIME_FRACTAL;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /**
     * A complex state stepped from every point of the plane until it settles: every point is coloured by where it
     * settles and how fast — Newton's method when the step is {@code z - p(z) / diff(p(z), z)}, though the engine does
     * not know that. The input is the state.
     *
     * @param state     the variable the step advances, which is also the input that ranges over the rectangle
     * @param iteration the step, with any local definitions ({@code p(z) = z^3 - 1}) in front of it
     * @param tolerance a step shorter than this has settled
     */
    record NewtonFractal(String state, String iteration, int maximumIterations, double tolerance, Interval real,
                         Interval imaginary) implements ObjectDefinition {
        public NewtonFractal {
            if (state == null || iteration == null || iteration.isBlank()) {
                throw new IllegalArgumentException("A basin iteration names its state and its step.");
            }
            if (maximumIterations < 1 || maximumIterations > 10_000) {
                throw new IllegalArgumentException("Iterations are between 1 and 10000.");
            }
            if (!(tolerance > 0)) {
                tolerance = 1e-9;
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.NEWTON_FRACTAL;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /** A set of contractions chosen at random (the chaos game): Sierpinski, Barnsley's fern, the Menger sponge. */
    record IteratedFunctionSystem(int dimension, List<AffineMap> maps) implements ObjectDefinition {
        public IteratedFunctionSystem {
            maps = List.copyOf(maps);
            if (dimension < 2 || dimension > 3 || maps.isEmpty() || maps.size() > 64) {
                throw new IllegalArgumentException("An IFS lives in 2 or 3 dimensions and has 1–64 maps.");
            }
            for (AffineMap map : maps) {
                if (map.matrix().size() != dimension || map.translation().size() != dimension) {
                    throw new IllegalArgumentException("Every map of the system is " + dimension + "-dimensional.");
                }
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.ITERATED_FUNCTION_SYSTEM;
        }

        public int ambientDimension() {
            return dimension;
        }
    }

    record AffineMap(List<List<Double>> matrix, List<Double> translation, double weight) {
        public AffineMap {
            matrix = matrix.stream().map(List::copyOf).toList();
            translation = List.copyOf(translation);
            if (!(weight > 0)) {
                weight = 1;
            }
        }
    }

    /** A rewriting system drawn with a turtle: Koch, the dragon curve, plants. */
    record LindenmayerSystem(String axiom, Map<String, String> rules, double angle, int iterations)
            implements ObjectDefinition {
        public LindenmayerSystem {
            rules = Map.copyOf(rules);
            if (axiom == null || axiom.isBlank() || iterations < 0 || iterations > 12) {
                throw new IllegalArgumentException("An L-system needs an axiom and 0–12 iterations.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.LINDENMAYER_SYSTEM;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /**
     * A matrix whose entries may be expressions of the object's parameters.
     *
     * <p>A square matrix whose rows are probability distributions is a Markov chain's transitions; its matrix analysis
     * carries a distribution step by step when the document asks it to ({@code from … side LEFT}), and finds the
     * stationary one as the eigenvector of eigenvalue 1 — the engine knows the matrix, never the chain.</p>
     *
     * @param labels what the rows stand for, in order — the states of a chain; empty when they are only numbered
     */
    record MatrixObject(List<List<String>> entries, List<String> labels) implements ObjectDefinition {
        public MatrixObject {
            entries = entries.stream().map(List::copyOf).toList();
            labels = labels == null ? List.of() : List.copyOf(labels);
            if (entries.isEmpty() || entries.size() > 8) {
                throw new IllegalArgumentException("A matrix has 1–8 rows.");
            }
            int columns = entries.getFirst().size();
            if (columns == 0 || columns > 8 || entries.stream().anyMatch(row -> row.size() != columns)) {
                throw new IllegalArgumentException("Every row of a matrix has the same 1–8 columns.");
            }
            if (!labels.isEmpty() && labels.size() != entries.size()) {
                throw new IllegalArgumentException("A matrix labels every row or none.");
            }
        }

        public MatrixObject(List<List<String>> entries) {
            this(entries, List.of());
        }

        public ObjectKind objectKind() {
            return ObjectKind.MATRIX;
        }

        public int ambientDimension() {
            return entries.size();
        }
    }

    /** x ↦ A x + b: translation, rotation, scaling, shear and every affine map. */
    record AffineTransformation(List<List<String>> matrix, List<String> translation) implements ObjectDefinition {
        public AffineTransformation {
            matrix = matrix.stream().map(List::copyOf).toList();
            translation = translation == null || translation.isEmpty()
                    ? Collections.nCopies(matrix.size(), "0") : List.copyOf(translation);
            int size = matrix.size();
            if (size < 2 || size > 4 || matrix.stream().anyMatch(row -> row.size() != size) || translation.size() != size) {
                throw new IllegalArgumentException("An affine map is a square 2×2, 3×3 or 4×4 matrix with a translation.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.AFFINE_TRANSFORMATION;
        }

        public int ambientDimension() {
            return matrix.size();
        }
    }

    /** x ↦ G(x) with one expression per coordinate: inversion in a circle, a polar warp. */
    record NonlinearTransformation(List<String> components, List<Interval> bounds) implements ObjectDefinition {
        public NonlinearTransformation {
            components = List.copyOf(components);
            bounds = List.copyOf(bounds);
            if (components.size() < 2 || components.size() > 3 || bounds.size() != components.size()) {
                throw new IllegalArgumentException("A nonlinear map has 2 or 3 components and one bound per axis.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.NONLINEAR_TRANSFORMATION;
        }

        public int ambientDimension() {
            return components.size();
        }
    }

    /** Points given by their coordinates. */
    record PointSet(int dimension, List<List<Double>> points) implements ObjectDefinition {
        public PointSet {
            points = points.stream().map(List::copyOf).toList();
            if (dimension < 1 || dimension > 8 || points.size() > 100_000
                    || points.stream().anyMatch(point -> point.size() != dimension)) {
                throw new IllegalArgumentException("Every point has " + dimension + " coordinates; at most 100000 points.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.POINT_SET;
        }

        public int ambientDimension() {
            return dimension;
        }
    }

    /** Constructed geometry: points, lines, circles, planes, spheres and what is derived from them. */
    record GeometricConstruction(int dimension, List<GeometricElement> elements) implements ObjectDefinition {
        public GeometricConstruction {
            elements = List.copyOf(elements);
            if (dimension < 2 || dimension > 3 || elements.size() > 500) {
                throw new IllegalArgumentException("A construction is 2D or 3D with at most 500 elements.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.GEOMETRIC_CONSTRUCTION;
        }

        public int ambientDimension() {
            return dimension;
        }
    }

    /**
     * One element of a construction. A free POINT carries `coordinates` (expressions of the parameters); every
     * derived element names the elements it is built from in `references`, in the order its type expects.
     */
    record GeometricElement(String id, GeometricElementType type, List<String> references, List<String> coordinates,
                            String label) {
        public GeometricElement {
            references = references == null ? List.of() : List.copyOf(references);
            coordinates = coordinates == null ? List.of() : List.copyOf(coordinates);
            if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_]{0,31}")) {
                throw new IllegalArgumentException("An element id is a name such as A or l1.");
            }
        }
    }

    /**
     * Vertices and the edges between them, in 2 to 8 dimensions. Nothing here names a family: a hypercube is the
     * vertices {@code cartesian(-1, 1, dimension)} joined where {@code hamming(first, second) = 1}, and the polytope
     * engine evaluates the rule and the condition — see {@code HigherDimensionComputations.Skeleton}.
     *
     * @param vertexRule    a vertex rule the engine supplies, as written ({@code cartesian(-1, 1, dimension)}); null when
     *                      the vertices are listed
     * @param vertices      the vertices listed, each with {@code dimension} coordinates; empty when a rule makes them
     * @param edgeCondition what joins two vertices {@code first} and {@code second}, as written; null when the edges are
     *                      listed, or when neither is written and the shortest distances are the edges
     * @param edges         the pairs of vertex indices listed
     */
    record Polytope(int dimension, String vertexRule, List<List<Double>> vertices, String edgeCondition,
                    List<List<Integer>> edges) implements ObjectDefinition {
        public Polytope {
            vertices = vertices == null ? List.of() : vertices.stream().map(List::copyOf).toList();
            edges = edges == null ? List.of() : edges.stream().map(List::copyOf).toList();
            vertexRule = vertexRule == null || vertexRule.isBlank() ? null : vertexRule;
            edgeCondition = edgeCondition == null || edgeCondition.isBlank() ? null : edgeCondition;
            if (dimension < 2 || dimension > 8) {
                throw new IllegalArgumentException("A polytope lives in 2 to 8 dimensions.");
            }
            if ((vertexRule == null) == vertices.isEmpty()) {
                throw new IllegalArgumentException("A polytope's vertices are made by a rule or listed, one of the two.");
            }
            if (edgeCondition != null && !edges.isEmpty()) {
                throw new IllegalArgumentException("A polytope's edges are a condition or a list, not both.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.POLYTOPE;
        }

        public int ambientDimension() {
            return dimension;
        }
    }

    /**
     * A density over one or two variables: a curve or a surface whose output is of type {@code density}. Nothing here
     * names a family — the normal distribution is its formula — and the density engine draws the samples, the
     * cumulative function and the moments from the expression alone.
     *
     * @param density the density as written over {@code variables}, with any local definitions in front of it
     * @param support where the density is drawn and sampled, one interval per variable
     */
    record ProbabilityDistribution(List<String> variables, String density, List<Interval> support)
            implements ObjectDefinition {
        public ProbabilityDistribution {
            variables = variables == null || variables.isEmpty() ? List.of("x") : List.copyOf(variables);
            support = support == null ? List.of() : List.copyOf(support);
            if (variables.size() > 2) {
                throw new IllegalArgumentException("A distribution here has one or two variables.");
            }
            if (density == null || density.isBlank()) {
                throw new IllegalArgumentException("A distribution writes its density.");
            }
            if (support.size() != variables.size()) {
                throw new IllegalArgumentException("The support names one interval per variable.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.PROBABILITY_DISTRIBUTION;
        }

        public int ambientDimension() {
            return variables.size() + 1;
        }
    }

    /**
     * The points of the plane where a condition holds, drawn as a picture: {@code picture : image where …}.
     *
     * <p>Over two {@code integer} inputs every pixel is one pair of integers, and when the document has {@code exact}
     * parameters the condition is decided in exact fractions — Tupper's inequality, whose {@code k} is 543 digits long,
     * is decided without one rounding error. Over real inputs the plane is sampled at a resolution.</p>
     *
     * <p>The picture is drawn as a plot: the first input runs from its range's first bound on the left to its second
     * on the right, the second from its first bound at the bottom to its second at the top. A range may run downwards
     * ({@code y in k + 16 .. k}), which turns that axis over.</p>
     *
     * @param condition     the condition, with any local definitions in front of it
     * @param variables     the two inputs, horizontal first
     * @param from          each input's first bound, as written: an expression of the parameters
     * @param to            each input's second bound, as written
     * @param integer       whether the inputs range over the integers, one pixel each
     * @param exactIntegers the {@code exact} parameters, their digits as written
     */
    record Region(String condition, List<String> variables, List<String> from, List<String> to, boolean integer,
                  Map<String, String> exactIntegers) implements ObjectDefinition {
        public Region {
            variables = List.copyOf(variables);
            from = List.copyOf(from);
            to = List.copyOf(to);
            exactIntegers = exactIntegers == null ? Map.of() : Map.copyOf(exactIntegers);
            if (condition == null || condition.isBlank()) {
                throw new IllegalArgumentException("A region writes the condition its points meet.");
            }
            if (variables.size() != 2 || from.size() != 2 || to.size() != 2) {
                throw new IllegalArgumentException("A region is drawn over two inputs, each with its range.");
            }
            if (!exactIntegers.isEmpty() && !integer) {
                throw new IllegalArgumentException("A region is decided exactly over integer inputs only.");
            }
            if (exactIntegers.size() > 16 || exactIntegers.entrySet().stream().anyMatch(entry ->
                    !entry.getKey().matches("[A-Za-z_][A-Za-z_0-9]*") || !entry.getValue().matches("-?[0-9]{1,4096}"))) {
                throw new IllegalArgumentException("A region has up to 16 exact integers of at most 4096 digits each.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.REGION;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /** a(n) for n in [first, last]: a curve over an integer input — a numeric set, a series' partial sums. */
    record Sequence(String expression, int first, int last, List<String> variables) implements ObjectDefinition {
        public Sequence {
            if (last < first || last - first > 100_000) {
                throw new IllegalArgumentException("A sequence covers at most 100000 terms, first ≤ last.");
            }
            variables = named(variables, List.of("n"), "A sequence");
        }

        public ObjectKind objectKind() {
            return ObjectKind.SEQUENCE;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /**
     * One number: an expression of the parameters alone — {@code 2^3^2}, the Basel sum, a quadratic's root. It has no
     * inputs, so there is nothing to plot; it is evaluated and shown as a measurement named after its output.
     *
     * @param name the output's name, which labels the number
     */
    record Value(String name, String expression) implements ObjectDefinition {
        public Value {
            if (name == null || name.isBlank() || expression == null || expression.isBlank()) {
                throw new IllegalArgumentException("A value names its output and writes its expression.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.VALUE;
        }

        public int ambientDimension() {
            return 1;
        }
    }

    /**
     * A lumped electrical circuit: elements between named nodes, node {@value #GROUND} being the reference. Every
     * value is a jMF expression over the object's parameters, so a slider can be a resistance; the circuit starts at
     * rest at the beginning of {@code time}, which is the span a transient response covers, in seconds.
     *
     * @param layout how the circuit is drawn — its {@code layout} block —, or null when a sheet lays it out itself;
     *               nothing in it changes what is computed
     * @param conditions what a bench runs it under — temperature, tolerance corner, source offsets —, or null for a
     *                   circuit run as it is written
     * @param probes     what a bench measures it with — its {@code probes} block: the oscilloscope's channels and the
     *                   meters —; like the layout, nothing in it changes what is computed
     * @param timePerDivision the time base the oscilloscope opens on, seconds a division — the {@code probes} block's
     *                        {@code time 5 ms/div}; null when the circuit names none and the bench chooses
     * @param boundary   what crosses the circuit's edge when it is a sheet of a project: its ports and its global nets
     */
    record Circuit(List<CircuitElement> elements, Interval time, SchematicLayout layout, BenchConditions conditions,
                   List<BenchProbe> probes, List<CircuitExpectation> expectations, Double timePerDivision,
                   SheetBoundary boundary)
            implements ObjectDefinition {

        public static final String GROUND = "0";

        /** A guard against a runaway document, not a limit of the solver: it splits and solves far larger circuits. */
        public static final int MAXIMUM_ELEMENTS = 2000;

        /**
         * Refuses a circuit that cannot be simulated yet — one with no parts, or none connected to ground —, saying
         * what it lacks. Everything that simulates a circuit asks this first; a circuit being drawn may lack either.
         */
        public void requireSolvable() {
            if (elements.isEmpty()) {
                throw new IllegalArgumentException("The circuit has no parts yet: place one and connect it.");
            }
            if (elements.stream().noneMatch(element -> element.connects(GROUND))) {
                throw new IllegalArgumentException("The circuit is not connected to ground: connect a part to a ground or a 0 V mark.");
            }
        }

        /** A circuit a sheet lays out by itself. */
        public Circuit(List<CircuitElement> elements, Interval time) {
            this(elements, time, null, null);
        }

        /** A circuit run as it is written: at 25 °C, every part nominal. */
        public Circuit(List<CircuitElement> elements, Interval time, SchematicLayout layout) {
            this(elements, time, layout, null);
        }

        /** A circuit without a bench's probes: what is computed needs none. */
        public Circuit(List<CircuitElement> elements, Interval time, SchematicLayout layout, BenchConditions conditions) {
            this(elements, time, layout, conditions, List.of());
        }

        /** A circuit that states nothing of itself to check: what is computed or run live needs no expectations. */
        public Circuit(List<CircuitElement> elements, Interval time, SchematicLayout layout, BenchConditions conditions,
                       List<BenchProbe> probes) {
            this(elements, time, layout, conditions, probes, List.of());
        }

        /** A circuit whose oscilloscope opens on whatever time base the bench chooses. */
        public Circuit(List<CircuitElement> elements, Interval time, SchematicLayout layout, BenchConditions conditions,
                       List<BenchProbe> probes, List<CircuitExpectation> expectations) {
            this(elements, time, layout, conditions, probes, expectations, null);
        }

        /** A circuit that is no sheet of a project: nothing crosses its edge. */
        public Circuit(List<CircuitElement> elements, Interval time, SchematicLayout layout, BenchConditions conditions,
                       List<BenchProbe> probes, List<CircuitExpectation> expectations, Double timePerDivision) {
            this(elements, time, layout, conditions, probes, expectations, timePerDivision, SheetBoundary.NONE);
        }

        /** The same circuit made of other parts — flattened, conditioned —, everything else about it kept. */
        public Circuit withElements(List<CircuitElement> replaced) {
            return new Circuit(replaced, time, layout, conditions, probes, expectations, timePerDivision, boundary);
        }

        public Circuit {
            boundary = boundary == null ? SheetBoundary.NONE : boundary;
            if (timePerDivision != null && !(timePerDivision > 0)) {
                throw new IllegalArgumentException("A time base is a positive time a division.");
            }
            probes = probes == null ? List.of() : List.copyOf(probes);
            expectations = expectations == null ? List.of() : List.copyOf(expectations);
            elements = elements == null ? List.of() : List.copyOf(elements);
            // A circuit is a document while it is being drawn: empty, or not yet connected to ground, it is kept as it
            // is; whether it can be simulated is asked where it is simulated (requireSolvable).
            if (elements.size() > MAXIMUM_ELEMENTS) {
                throw new IllegalArgumentException("A circuit has at most " + MAXIMUM_ELEMENTS + " elements.");
            }
            if (elements.stream().map(CircuitElement::id).distinct().count() != elements.size()) {
                throw new IllegalArgumentException("Every element of a circuit has its own id.");
            }
            if (time == null || time.minimum() != 0 || time.maximum() <= 0) {
                throw new IllegalArgumentException("A circuit's time span starts at 0 and lasts a positive time.");
            }
            if (conditions != null && conditions.nominal()) {
                conditions = null;
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.CIRCUIT;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /**
     * What crosses a sheet's edge, as KiCad has it: a <em>port</em> is a net the sheet hands to its parent — the
     * hierarchical label, a pin of the sheet's block on the parent sheet (its {@code ports} block) —, a <em>global</em>
     * net is one name across every sheet of the project (a {@code nets} line marked {@code global}: the rails). Every
     * other net of a sheet is its own, kept apart in each instance; node {@value Circuit#GROUND} is everybody's.
     *
     * @param ports   the nets handed to the parent, in the order written
     * @param globals the nets that are one net across the project
     */
    record SheetBoundary(List<SheetPort> ports, Set<String> globals) {

        public static final SheetBoundary NONE = new SheetBoundary(List.of(), Set.of());

        public SheetBoundary {
            ports = ports == null ? List.of() : List.copyOf(ports);
            globals = globals == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(globals));
            Set<String> named = new HashSet<>();
            for (SheetPort port : ports) {
                if (!named.add(port.net())) {
                    throw new IllegalArgumentException("Port '" + port.net() + "' is written twice.");
                }
                if (globals.contains(port.net())) {
                    throw new IllegalArgumentException("Net '" + port.net() + "' is a port and a global net at once: "
                            + "a global net already reaches every sheet.");
                }
            }
            if (globals.contains(Circuit.GROUND)) {
                throw new IllegalArgumentException("Node 0 is every sheet's already: it is not written as a global net.");
            }
        }

        public boolean isPort(String net) {
            return ports.stream().anyMatch(port -> port.net().equals(net));
        }

        @JsonIgnore
        public boolean isEmpty() {
            return ports.isEmpty() && globals.isEmpty();
        }
    }

    /**
     * A net a sheet hands to its parent, and which way its signal goes — what the block's pin is drawn as on the parent.
     *
     * @param direction {@code input}, {@code output}, {@code power}, {@code ground} or {@code analog}
     */
    record SheetPort(String net, String direction) {

        public static final Set<String> DIRECTIONS = Set.of("input", "output", "power", "ground", "analog");

        public SheetPort {
            if (net == null || !net.matches("[A-Za-z_][A-Za-z0-9_]{0,31}")) {
                throw new IllegalArgumentException("A port is named like a net: VIN, FB, clock.");
            }
            direction = direction == null ? "analog" : direction;
            if (!DIRECTIONS.contains(direction)) {
                throw new IllegalArgumentException("A port is input, output, power, ground or analog, not '" + direction + "'.");
            }
        }
    }

    /** Where a text of a placed part stands, from the part's centre. */
    record LabelOffset(double x, double y) {
    }

    /**
     * Where a circuit's parts, its ground and junction marks and its wires are drawn — the {@code layout} block.
     *
     * @param items every placed part and mark
     * @param wires every wire, from terminal to terminal through its corners
     */
    record SchematicLayout(List<PlacedItem> items, List<SchematicWire> wires) {
        public SchematicLayout {
            items = items == null ? List.of() : List.copyOf(items);
            wires = wires == null ? List.of() : List.copyOf(wires);
        }
    }

    /**
     * A probe of a bench, as the circuit's {@code probes} block writes it. {@code CH1}…{@code CH4} are the
     * oscilloscope's channels; any other id is a meter.
     *
     * @param measure   {@code voltage}, {@code current}, {@code power} or {@code level}
     * @param target    a node, or a part — a current's with its terminal, {@code PS1.negative}
     * @param reference a voltage's second node; null for ground
     * @param scale     an oscilloscope channel's volts per division; null when not written
     * @param mode      a meter's display: {@code dc}, {@code ac} or {@code rms}; null when not written
     * @param expression what an oscilloscope channel's screen shows, X being the reading; null when not written
     */
    record BenchProbe(String id, String measure, String target, String reference, Double scale, String mode, String expression) {
        public BenchProbe {
            if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_]{0,15}")) {
                throw new IllegalArgumentException("A probe is named like a part: CH1, M1.");
            }
            if (!Set.of("voltage", "current", "power", "level").contains(measure)) {
                throw new IllegalArgumentException("A probe measures voltage, current, power or level, not '" + measure + "'.");
            }
        }
    }

    /**
     * @param mark   {@code ground} (earth), {@code common} (the circuit's 0 V, drawn as such) or {@code junction} for a
     *               mark that is not a part; null for a part
     * @param net    the net a ground mark is ({@code agnd}); null for node 0 and for anything that is not a ground
     * @param labels where its texts stand when they were put somewhere by hand — {@code name}, {@code value},
     *               {@code readings} —, from its centre; empty when the sheet places them all
     * @param shows  how an instrument's display reads, when the reader chose ({@code rms}, {@code graph}); empty otherwise
     */
    record PlacedItem(String id, String mark, String net, double x, double y, double rotation, boolean mirrored,
                      Map<String, LabelOffset> labels, List<String> shows) {

        /** The texts a part has, each placed by hand or by the sheet. */
        private static final Set<String> TEXTS = Set.of("name", "value", "readings");

        /**
         * The marks a sheet places besides its parts: an earth, the circuit's 0 V, a junction, and the three labels of a
         * net — {@code label} is the flag that is its net wherever it stands on this sheet, so one net reaches across
         * the sheet without a wire drawn; {@code port} draws a net the sheet hands to its parent (its {@code ports}
         * block); {@code global} draws a net that is one name across the whole project.
         */
        private static final Set<String> MARKS = Set.of("ground", "common", "junction", "label", "port", "global");

        /** The marks that must name their net. */
        private static final Set<String> NAMED = Set.of("label", "port", "global");

        public PlacedItem(String id, String mark, String net, double x, double y, double rotation, boolean mirrored) {
            this(id, mark, net, x, y, rotation, mirrored, Map.of(), List.of());
        }

        public PlacedItem {
            labels = labels == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(labels));
            shows = shows == null ? List.of() : List.copyOf(shows);
            for (String label : labels.keySet()) {
                if (!TEXTS.contains(label)) {
                    throw new IllegalArgumentException("A part's texts are its name, its value and its readings, not '" + label + "'.");
                }
            }
            if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_]{0,31}")) {
                throw new IllegalArgumentException("A placed item is named like a part: R1, GND1, J1.");
            }
            if (mark != null && !MARKS.contains(mark)) {
                throw new IllegalArgumentException("A mark is 'ground', 'common', 'junction', 'label', 'port' or 'global', not '"
                        + mark + "'.");
            }
            if (mark != null && NAMED.contains(mark) && (net == null || net.isBlank())) {
                throw new IllegalArgumentException("A " + mark + " names the net it is: '" + id + " " + mark + " vp at x, y'.");
            }
        }
    }

    /**
     * @param from a terminal: {@code R1.positive}, {@code U1.output}, {@code GND1.ground}, {@code J1.j}
     * @param to   another terminal
     */
    record SchematicWire(String id, String from, String to, List<Bend> bends) {
        public SchematicWire {
            bends = bends == null ? List.of() : List.copyOf(bends);
        }
    }

    record Bend(double x, double y) {
    }

    /**
     * Formulas and the program that processes them: jMF definitions ({@code f(x) = …}), callable by name inside a
     * jMS program, which runs with the object's parameters as its variables and returns what is shown — a number,
     * a list of numbers, a list of points, or a map of named values or columns.
     */
    record Program(String formulas, String program, Map<String, String> integerInputs) implements ObjectDefinition {
        public Program {
            formulas = formulas == null ? "" : formulas;
            integerInputs = integerInputs == null ? Map.of() : Map.copyOf(integerInputs);
            if (program == null || program.isBlank()) {
                throw new IllegalArgumentException("A program object has a program.");
            }
            if (program.length() > 50_000 || formulas.length() > 50_000) {
                throw new IllegalArgumentException("A program and its formulas stay under 50000 characters each.");
            }
            if (integerInputs.size() > 16 || integerInputs.entrySet().stream().anyMatch(entry ->
                    !entry.getKey().matches("[A-Za-z_][A-Za-z_0-9]*") || !entry.getValue().matches("-?[0-9]{1,4096}"))) {
                throw new IllegalArgumentException("A program has up to 16 named exact integers of at most 4096 digits each.");
            }
        }

        public Program(String formulas, String program) {
            this(formulas, program, Map.of());
        }

        public ObjectKind objectKind() {
            return ObjectKind.PROGRAM;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /**
     * A component as its jMX document: pins, symbol, jMF equations, jMS behaviour and the datasheet points it must meet. It
     * is read when saved, so a model that does not parse is refused with its line; a circuit uses it by reference.
     */
    record Component(String source) implements ObjectDefinition {
        public Component {
            if (source == null || source.isBlank()) {
                throw new IllegalArgumentException("A component has a jMX document.");
            }
            if (source.length() > 200_000) {
                throw new IllegalArgumentException("A component model stays under 200000 characters.");
            }
            org.jmouse.parallax.script.ExperienceReader.readOne(source, "component");
        }

        public ObjectKind objectKind() {
            return ObjectKind.COMPONENT;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /** A reusable vector symbol: the drawing its jMX document describes (size, paths, pins, labels, indicators), carried as {@code SymbolDrawing} JSON. */
    record Symbol(String drawing) implements ObjectDefinition {
        public Symbol {
            if (drawing == null || drawing.isBlank() || drawing.length() > 100_000) {
                throw new IllegalArgumentException("A symbol needs a drawing under 100000 characters.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.SYMBOL;
        }

        public int ambientDimension() {
            return 2;
        }
    }


    /**
     * Lines drawn together on panes and moved by one clock — the Graph workbench saved. Nothing is computed for the
     * graph itself: each line is an expression the workbench classifies and computes, in the pane it names.
     *
     * @param formulas the graph's own definitions, prepended to every line ({@code f(x) = …})
     * @param clock    the parameter every pane moves with; null when nothing plays
     * @param layout   {@code ROWS}, {@code COLUMNS} or {@code GRID}
     * @param panes    at least one; a graph written without panes has one plane, {@code main}
     */
    record Graph(String formulas, GraphClock clock, String layout, List<GraphPane> panes, List<GraphLine> lines,
                 List<GraphCheck> checks) implements ObjectDefinition {
        public Graph {
            formulas = formulas == null ? "" : formulas;
            panes = List.copyOf(panes);
            lines = List.copyOf(lines);
            checks = checks == null ? List.of() : List.copyOf(checks);
            if (panes.isEmpty()) {
                throw new IllegalArgumentException("A graph has at least one pane.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.GRAPH;
        }

        public int ambientDimension() {
            return 2;
        }
    }

    /**
     * One member for every whole value of {@code index} from {@code from} to {@code to} — a graph's line
     * {@code y = sin((2k - 1) x) / (2k - 1)  k in 1 .. N integer}. The member reads the index as a parameter; the bounds
     * are expressions over the family's parameters, rounded to whole numbers when it is computed.
     */
    record Family(String index, String from, String to, ObjectDefinition member) implements ObjectDefinition {
        public Family {
            if (index == null || !index.matches("[A-Za-z][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("A family's index is a name such as k.");
            }
            if (member instanceof Family) {
                throw new IllegalArgumentException("A family of families is written as one family with a larger range.");
            }
        }

        public ObjectKind objectKind() {
            return ObjectKind.FAMILY;
        }

        public int ambientDimension() {
            return member.ambientDimension();
        }
    }

    /**
     * What a graph claims: {@code measure} must stay within {@code minimum .. maximum} at every frame of the clock;
     * {@code criterion} is as written ({@code near 1.7135 +- 0.5%}), the bounds are what it admits.
     */
    record GraphCheck(String title, String measure, String criterion, double minimum, double maximum) {
    }

    /**
     * What a circuit states of itself — its {@code expect} blocks —: {@code measure} as written after the word
     * ({@code final out}, {@code operating current V1}), and the window it must fall in, or the exact text for {@code is}.
     * The circuit is its own bench; Model checks run it and measure it.
     */
    record CircuitExpectation(String title, String reference, List<String> measure, String criterion, double minimum,
                              double maximum, String exact) {
        public CircuitExpectation {
            measure = List.copyOf(measure);
        }
    }

    /**
     * A picture in which every pixel is one run of a differential system: started from the pixel ({@code horizontal},
     * {@code vertical} name it in the expressions), stepped in t until a stop holds or {@code crossing} changes sign
     * where {@code where} holds, and shaded by {@code value} at that hit. Expressions are jMF over the state, t, the
     * pixel and the parameters; a condition is 1 or 0.
     */
    record FlowImage(String horizontal, Interval horizontalRange, String vertical, Interval verticalRange, List<String> starts,
                     List<String> stops, String crossing, String where, String value) {
        public FlowImage {
            starts = List.copyOf(starts);
            stops = List.copyOf(stops);
        }
    }

    /** {@code clock t from 0 to 1 speed 1 loop}; {@code from}/{@code to} are null when the parameter's range is used. */
    record GraphClock(String parameter, String from, String to, double speed, boolean loop) {
    }

    /**
     * A named view: its kind ({@code PLANE}, {@code SPACE}, {@code CIRCUIT}, {@code VALUES}) and the axes it fixes; a
     * circuit pane's {@code over} is the span of circuit time the clock's range is laid onto, as written.
     */
    record GraphPane(String name, String kind, Map<String, List<String>> axes, String uses, List<String> over, String camera) {
    }

    /** One line of a graph; {@code label} when the expression is a text whose {@code {…}} parts are read live. */
    record GraphLine(String name, String expression, boolean label, String until, String at, String from, String pane,
                     Integer color, boolean hidden) {
    }
    /**
     * What a circuit is run under on a bench, on top of what it is written as: the temperature, which corner of their
     * tolerances the parts sit at, a part's own tolerance and temperature coefficient where it is not the default, and
     * a DC offset on a source. The engine applies them when it builds the circuit ({@code BenchConditioning}); an
     * interface only says what they are.
     *
     * @param temperature °C, −40…125; parts are nominal at 25
     * @param corner      −1 every part at the low end of its tolerance, 0 nominal, +1 the high end
     * @param tolerances  a resistor's or capacitor's own tolerance and coefficient, by element id
     * @param offsets     a DC value added to a source, by element id, as a jMF expression
     * @param realistic   whether a part that overheats past its component's {@code T_maximum} fails, as its component
     *                    says it fails ({@code failure open|short}); an ideal bench only warns
     * @param failures    the parts that have failed, by id, and how, as their components say they fail
     */
    record BenchConditions(double temperature, int corner, Map<String, Tolerance> tolerances, Map<String, String> offsets,
                           boolean realistic, Map<String, Failure> failures, Detail detail) {

        /**
         * How much of the physics a run computes, and how finely: each part's equations read it as {@code detail}
         * (0, 1, 2) and decide what it changes — a diode without its junction's charge on a draft bench —, and the engine
         * solves to the precision it names. Datasheet checks and the comparison with SPICE run at {@link #STANDARD}.
         */
        public enum Detail {
            /** Fast and rough: the effects a document reserves for detail left out, a looser error per step. */
            DRAFT(0),
            /** What every check is held to: the documents' full physics at SPICE's own tolerances. */
            STANDARD(1),
            /** Finer: an error per step ten times tighter, and whatever a document adds at this level. */
            PRECISE(2);

            private final int figure;

            Detail(int figure) {
                this.figure = figure;
            }

            /** What a part's equations read as {@code detail}. */
            public int figure() {
                return figure;
            }
        }

        /** These conditions at the level of detail every check runs at. */
        public BenchConditions(double temperature, int corner, Map<String, Tolerance> tolerances, Map<String, String> offsets,
                               boolean realistic, Map<String, Failure> failures) {
            this(temperature, corner, tolerances, offsets, realistic, failures, Detail.STANDARD);
        }

        /**
         * How a part fails, as its component writes it: {@code failure open} — nothing conducts any more —, or
         * {@code failure short collector emitter} — those pins tied together, the rest left open; a short naming no pins
         * ties every pin together.
         *
         * @param kind how it fails
         * @param pins the pins a short ties, in the component's names; empty for every pin, and for an open part
         */
        public record Failure(Kind kind, List<String> pins) {

            /** The two ways a part fails, as a component writes them: {@code open} and {@code short}. */
            public enum Kind {
                OPEN, SHORT;

                /** As a component writes it, and as a reading reports it. */
                public String keyword() {
                    return name().toLowerCase(java.util.Locale.ROOT);
                }

                /** The kind a component writes; refused for any other word. */
                public static Kind of(String keyword) {
                    for (Kind kind : values()) {
                        if (kind.keyword().equals(keyword)) {
                            return kind;
                        }
                    }
                    throw new IllegalArgumentException("A part fails 'open' or 'short', not '" + keyword + "'.");
                }
            }

            /** A part that names no way to fail fails open. */
            public static final Failure OPEN = new Failure(Kind.OPEN, List.of());

            public Failure {
                pins = pins == null ? List.of() : List.copyOf(pins);
                if (kind == Kind.OPEN && !pins.isEmpty()) {
                    throw new IllegalArgumentException("An open part conducts nowhere: 'failure open' names no pins.");
                }
            }

            /** A failure as a component's header writes it: {@code open}, {@code short}, {@code short drain source}. */
            public static Failure of(List<String> words) {
                return new Failure(Kind.of(words.getFirst()), words.subList(1, words.size()));
            }

            public boolean shorts() {
                return kind == Kind.SHORT;
            }
        }

        /** An ideal bench: nothing fails. */
        public BenchConditions(double temperature, int corner, Map<String, Tolerance> tolerances, Map<String, String> offsets) {
            this(temperature, corner, tolerances, offsets, false, Map.of());
        }

        /** These conditions with one more part failed. */
        public BenchConditions withFailure(String part, Failure failure) {
            Map<String, Failure> all = new LinkedHashMap<>(failures);
            all.put(part, failure);
            return new BenchConditions(temperature, corner, tolerances, offsets, realistic, all, detail);
        }

        public static final double NOMINAL_TEMPERATURE = 25;

        /**
         * @param percent     ±%, 0…30
         * @param coefficient ppm/°C, within ±2000
         */
        public record Tolerance(double percent, double coefficient) {


            public Tolerance {
                if (!Double.isFinite(percent) || percent < 0 || percent > 30 || !Double.isFinite(coefficient)
                        || Math.abs(coefficient) > 2000) {
                    throw new IllegalArgumentException("A tolerance is 0…30 % and a coefficient within ±2000 ppm/°C.");
                }
            }
        }

        public BenchConditions {
            if (!Double.isFinite(temperature) || temperature < -40 || temperature > 125) {
                throw new IllegalArgumentException("The bench temperature is −40…125 °C.");
            }
            if (corner < -1 || corner > 1) {
                throw new IllegalArgumentException("A tolerance corner is −1, 0 or +1.");
            }
            tolerances = tolerances == null ? Map.of() : Map.copyOf(tolerances);
            Map<String, String> given = new LinkedHashMap<>();
            if (offsets != null) {
                offsets.forEach((id, offset) -> {
                    if (offset != null && !offset.isBlank() && !offset.strip().equals("0")) {
                        given.put(id, offset.strip());
                    }
                });
            }
            offsets = Map.copyOf(given);
            failures = failures == null ? Map.of() : Map.copyOf(failures);
            detail = detail == null ? Detail.STANDARD : detail;
        }

        /** Whether these conditions change nothing: what a circuit without any is run under. */
        public boolean nominal() {
            return temperature == NOMINAL_TEMPERATURE && corner == 0 && offsets.isEmpty() && !realistic && failures.isEmpty()
                    && detail == Detail.STANDARD;
        }

    }

    /**
     * One part of a circuit. A part written in pin order is a primitive, joining {@code positive} to {@code negative},
     * its {@code value} its component's first parameter (ohms, farads, henries, volts, amperes; a diode's saturation
     * current); a model names its {@code component} and connects its {@code pins}. Every circuit starts from rest, so a
     * source is switched on at the start; a sine or a square is a generator, a model of its own.
     */
    record CircuitElement(String id, CircuitElementType type, String positive, String negative, String value,
                          String component, Map<String, String> pins, Map<String, String> overrides, String symbol) {

        /** An element drawn with its component's default symbol. */
        public CircuitElement(String id, CircuitElementType type, String positive, String negative, String value,
                              String component, Map<String, String> pins, Map<String, String> overrides) {
            this(id, type, positive, negative, value, component, pins, overrides, null);
        }

        public CircuitElement {
            symbol = symbol == null || symbol.isBlank() ? null : symbol.strip();
            if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_]{0,31}")) {
                throw new IllegalArgumentException("An element id is a name such as R1 or V_in.");
            }
            if (type == null) {
                throw new IllegalArgumentException("Element '" + id + "' says what it is.");
            }
            pins = pins == null ? Map.of() : Map.copyOf(pins);
            overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
            if (type == CircuitElementType.MODEL) {
                if (component == null || component.isBlank()) {
                    throw new IllegalArgumentException("Model '" + id + "' names the component it is.");
                }
                if (pins.isEmpty()) {
                    throw new IllegalArgumentException("Model '" + id + "' connects its pins to nodes.");
                }
                positive = null;
                negative = null;
                value = value == null ? "" : value;
            } else if (positive == null || negative == null || positive.isBlank() || negative.isBlank() || positive.equals(negative)) {
                throw new IllegalArgumentException("Element '" + id + "' joins two different nodes.");
            }
            if (type != CircuitElementType.MODEL && (value == null || value.isBlank())) {
                throw new IllegalArgumentException("Element '" + id + "' has a value.");
            }
        }

        public boolean connects(String node) {
            return type == CircuitElementType.MODEL ? pins.containsValue(node) : positive.equals(node) || negative.equals(node);
        }
    }

    enum CircuitElementType {
        RESISTOR, CAPACITOR, INDUCTOR, VOLTAGE_SOURCE, CURRENT_SOURCE, DIODE,
        /** A component described by its jMX document, connected pin by pin; its reference is {@code name} for a built-in
         *  model or {@code slug@revision} for one in a library. */
        MODEL;

        public boolean isSource() {
            return this == VOLTAGE_SOURCE || this == CURRENT_SOURCE;
        }

        /** The component a primitive stands for — its parameters, ratings and drawing; null for a model, which names its own. */
        public String primitiveComponent() {
            return switch (this) {
                case RESISTOR -> "resistor";
                case CAPACITOR -> "capacitor";
                case INDUCTOR -> "inductor";
                case DIODE -> "diode";
                case VOLTAGE_SOURCE -> "voltage-source";
                case CURRENT_SOURCE -> "current-source";
                case MODEL -> null;
            };
        }

        /** The primitive a component stands for, by its slug or its {@code type}: {@code resistor} is a RESISTOR. */
        public static java.util.Optional<CircuitElementType> ofComponent(String component) {
            return java.util.Arrays.stream(values()).filter(type -> type != MODEL && type.primitiveComponent().equals(component)).findFirst();
        }
    }

    enum SurfaceGluing { NONE, CYLINDER, TORUS, MOBIUS, KLEIN_BOTTLE, PROJECTIVE_PLANE, SPHERE }

    enum GeometricElementType {
        POINT, MIDPOINT, LINE, SEGMENT, RAY, CIRCLE, CIRCLE_THROUGH_THREE, PLANE, SPHERE,
        INTERSECTION, PERPENDICULAR, PARALLEL, TANGENT, REFLECTION,
        /** A segment with a head at its second point: a force, a phasor, a velocity. */
        VECTOR,
        /** A closed polygon through its points, in order: a block, a wedge, a body. */
        POLYGON,
        /** An element turned about a point by an angle in radians: {@code rotation(shape, centre, angle)}. */
        ROTATION
    }
}
