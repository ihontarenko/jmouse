package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.fractal.FractalComputations;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.AffineMap;
import org.jmouse.parallax.object.ObjectDefinition.AffineTransformation;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.object.ObjectDefinition.ComplexMapping;
import org.jmouse.parallax.object.ObjectDefinition.Component;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.DiscreteMap;
import org.jmouse.parallax.object.ObjectDefinition.EscapeTimeFractal;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitCurve;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitSurface;
import org.jmouse.parallax.object.ObjectDefinition.GeometricConstruction;
import org.jmouse.parallax.object.ObjectDefinition.GeometricElement;
import org.jmouse.parallax.object.ObjectDefinition.GeometricElementType;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitCurve;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitHypersurface;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitSurface;
import org.jmouse.parallax.object.ObjectDefinition.IteratedFunctionSystem;
import org.jmouse.parallax.object.ObjectDefinition.LindenmayerSystem;
import org.jmouse.parallax.object.ObjectDefinition.MatrixObject;
import org.jmouse.parallax.object.ObjectDefinition.NewtonFractal;
import org.jmouse.parallax.object.ObjectDefinition.NonlinearTransformation;
import org.jmouse.parallax.object.ObjectDefinition.ParametricCurve;
import org.jmouse.parallax.object.ObjectDefinition.ParametricSurface;
import org.jmouse.parallax.object.ObjectDefinition.PointSet;
import org.jmouse.parallax.object.ObjectDefinition.Polytope;
import org.jmouse.parallax.object.ObjectDefinition.ProbabilityDistribution;
import org.jmouse.parallax.object.ObjectDefinition.Program;
import org.jmouse.parallax.object.ObjectDefinition.Region;
import org.jmouse.parallax.object.ObjectDefinition.Sequence;
import org.jmouse.parallax.object.ObjectDefinition.SurfaceGluing;
import org.jmouse.parallax.object.ObjectDefinition.Symbol;
import org.jmouse.parallax.object.ObjectDefinition.Value;
import org.jmouse.parallax.object.ObjectDefinition.VectorField;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Equation;
import org.jmouse.parallax.script.Block.EquationForm;
import org.jmouse.parallax.script.Block.OutputType;
import org.jmouse.parallax.script.Block.Parameter;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta.MetaValue;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.parallax.script.engine.DefinitionWords.Primitive;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static org.jmouse.parallax.script.engine.DefinitionWords.AXES;

/**
 * An {@link ObjectDefinition} as the blocks of the jMX document that says it, by the mapping table in
 * {@code JMX-MAPPING.md}: the kind, the constants it keeps in {@code parameters}, the fields it keeps in {@code meta}
 * and its own blocks — the model a document is, which {@link org.jmouse.parallax.script.writer.DocumentWriter} renders.
 * Expressions travel as the definition holds them; only numbers the definition keeps as numbers are spelled, by
 * {@link Notation#number}.
 *
 * <p>A {@link Component} is not mapped here: its source is the whole document, and {@link ExperienceDefinitions}
 * returns it as it is.</p>
 */
final class DefinitionBlocks {

    /**
     * What a definition contributes to its document.
     *
     * @param constants  parameters without a range — {@code bailout = 2}, {@code dimension = 4 integer}: values of the
     *                   definition, not sliders
     * @param metaFields meta fields the definition owns, such as a matrix's row {@code labels}
     * @param blocks     the definition's blocks, in the order they are written
     */
    record Mapped(Kind kind, List<Parameter> constants, Map<String, MetaValue> metaFields, List<Block> blocks) {

        Mapped {
            constants = List.copyOf(constants);
            metaFields = new LinkedHashMap<>(metaFields);
            blocks = List.copyOf(blocks);
        }

        Mapped(Kind kind, List<Block> blocks) {
            this(kind, List.of(), Map.of(), blocks);
        }
    }

    /** The word of a {@code probes} line that sets the oscilloscope's time base. */
    static final String TIME_BASE = "time";

    /** The units a time base is spelled in, the largest that keeps its figure at one or more. */
    private static final List<String> TIME_UNITS = List.of("s", "ms", "µs", "ns");

    private DefinitionBlocks() {
    }

    static Mapped of(ObjectDefinition definition) {
        return switch (definition) {
            case ExplicitCurve curve -> explicitCurve(curve);
            case ParametricCurve curve -> parametricCurve(curve);
            case ImplicitCurve curve -> implicitCurve(curve);
            case ExplicitSurface surface -> explicitSurface(surface);
            case ParametricSurface surface -> parametricSurface(surface);
            case ImplicitSurface surface -> implicitSurface(surface);
            case ImplicitHypersurface hypersurface -> implicitHypersurface(hypersurface);
            case VectorField field -> vectorField(field);
            case DifferentialSystem system -> differentialSystem(system);
            case DiscreteMap map -> discreteMap(map);
            case ComplexMapping mapping -> complexMapping(mapping);
            case EscapeTimeFractal fractal -> escapeTimeFractal(fractal);
            case NewtonFractal fractal -> newtonFractal(fractal);
            case IteratedFunctionSystem system -> iteratedFunctionSystem(system);
            case LindenmayerSystem system -> lindenmayerSystem(system);
            case MatrixObject matrix -> matrix(matrix);
            case AffineTransformation transformation -> affineTransformation(transformation);
            case NonlinearTransformation transformation -> nonlinearTransformation(transformation);
            case PointSet points -> pointSet(points);
            case GeometricConstruction construction -> construction(construction);
            case Polytope polytope -> polytope(polytope);
            case ProbabilityDistribution distribution -> distribution(distribution);
            case Region region -> region(region);
            case Sequence sequence -> sequence(sequence);
            case Value value -> value(value);
            case Program program -> program(program);
            case Circuit circuit -> circuit(circuit);
            case Symbol symbol -> new Mapped(Kind.SYMBOL, List.of(symbol(symbol.drawing())));
            case ObjectDefinition.Graph graph -> graph(graph);
            case ObjectDefinition.Family family -> throw new IllegalArgumentException(
                    "A family is a line of a graph; it is kept as the graph's text, not as an object of its own.");
            case Component component -> throw new IllegalArgumentException(
                    "A component is written as its own source, not assembled from blocks.");
        };
    }

    // ─── curves and surfaces ────────────────────────────────────────────────────────────────────────

    private static Mapped explicitCurve(ExplicitCurve curve) {
        return single(Kind.CURVE, List.of(input(curve.variables().getFirst(), curve.x())), "y", OutputType.CURVE,
                curve.expression());
    }

    private static Mapped parametricCurve(ParametricCurve curve) {
        Block.Input parameter = new Block.Input(List.of(curve.variables().getFirst()), Optional.of(range(curve.t())),
                Optional.empty(), false, curve.closed(), false, "");
        return new Mapped(Kind.CURVE, List.of(inputs(List.of(parameter)),
                outputs(output("trace", OutputType.CURVE, Optional.empty(), curve.components()))));
    }

    private static Mapped implicitCurve(ImplicitCurve curve) {
        List<String> names = curve.variables();
        return implicit(Kind.CURVE, List.of(input(names.get(0), curve.x()), input(names.get(1), curve.y())),
                OutputType.CURVE, curve.expression());
    }

    private static Mapped explicitSurface(ExplicitSurface surface) {
        List<String> names = surface.variables();
        return single(Kind.SURFACE, List.of(input(names.get(0), surface.x()), input(names.get(1), surface.y())), "z",
                OutputType.SURFACE, surface.expression());
    }

    private static Mapped parametricSurface(ParametricSurface surface) {
        List<String> names = surface.variables();
        Optional<String> gluing = surface.gluing() == SurfaceGluing.NONE ? Optional.empty()
                : Optional.of(DefinitionWords.word(surface.gluing()));
        return new Mapped(Kind.SURFACE, List.of(inputs(List.of(input(names.get(0), surface.u()), input(names.get(1), surface.v()))),
                outputs(output("shape", OutputType.SURFACE, gluing, surface.components()))));
    }

    private static Mapped implicitSurface(ImplicitSurface surface) {
        List<String> names = surface.variables();
        return implicit(Kind.SURFACE, List.of(input(names.get(0), surface.x()), input(names.get(1), surface.y()),
                input(names.get(2), surface.z())), OutputType.SURFACE, surface.expression());
    }

    private static Mapped implicitHypersurface(ImplicitHypersurface hypersurface) {
        return implicit(Kind.HYPERSURFACE, List.of(input("x", hypersurface.x()), input("y", hypersurface.y()),
                input("z", hypersurface.z()), input("w", hypersurface.w())), OutputType.VOLUME, hypersurface.expression());
    }

    // ─── fields and dynamics ────────────────────────────────────────────────────────────────────────

    private static Mapped vectorField(VectorField field) {
        return new Mapped(Kind.FIELD, List.of(inputs(axisInputs(field.bounds())),
                outputs(output("flow", OutputType.FIELD, Optional.empty(), field.components()))));
    }

    private static Mapped differentialSystem(DifferentialSystem system) {
        List<Equation> equations = new ArrayList<>();
        for (int index = 0; index < system.variables().size(); index++) {
            equations.add(new Equation(EquationForm.DERIVATIVE, system.variables().get(index), null, null,
                    written(system.equations().get(index))));
        }
        return new Mapped(Kind.DYNAMICS, List.of(state(system.variables(), system.starts(), system.bounds()),
                new Block.Equations(equations), inputs(List.of(input("t", system.time())))));
    }

    /** The step's values first, as {@code let} lines, then one update per variable. */
    private static Mapped discreteMap(DiscreteMap map) {
        List<Equation> equations = new ArrayList<>();
        map.stepValues().forEach(value -> equations.add(new Equation(EquationForm.LOCAL, value.name(), null, null,
                written(value.expression()))));
        for (int index = 0; index < map.variables().size(); index++) {
            equations.add(new Equation(EquationForm.NEXT, map.variables().get(index), null, null,
                    written(map.updates().get(index))));
        }
        return new Mapped(Kind.ITERATION, List.of(state(map.variables(), map.starts(), map.bounds()),
                new Block.Equations(equations)));
    }

    /** {@code x = 0 continuous in -25 .. 25}: where each variable starts, and its bounds when it has them. */
    private static Block.State state(List<String> variables, List<String> starts, List<Interval> bounds) {
        List<Block.StateVariable> lines = new ArrayList<>();
        for (int index = 0; index < variables.size(); index++) {
            Optional<Range> range = bounds.isEmpty() ? Optional.empty() : Optional.of(range(bounds.get(index)));
            lines.add(new Block.StateVariable(variables.get(index), written(starts.get(index)), false, range, "", 0));
        }
        return new Block.State(lines);
    }

    // ─── the complex plane ──────────────────────────────────────────────────────────────────────────

    private static Mapped complexMapping(ComplexMapping mapping) {
        return single(Kind.MAPPING, List.of(region(mapping.variables().getFirst(), mapping.real(), mapping.imaginary())),
                "w", OutputType.FIELD, mapping.expression());
    }

    /**
     * An {@code iteration} whose output folds every point's orbit into a picture: the state starts where its
     * {@code state} line says, or at the point when the input is the state, and the engine's limits are constants.
     */
    private static Mapped escapeTimeFractal(EscapeTimeFractal fractal) {
        List<Parameter> constants = List.of(constant(DefinitionWords.BAILOUT, Notation.number(fractal.bailout())),
                constant(DefinitionWords.ITERATIONS, String.valueOf(fractal.maximumIterations()), ParameterFlag.INTEGER));
        List<Block> blocks = new ArrayList<>();
        blocks.add(inputs(List.of(region(fractal.input(), fractal.real(), fractal.imaginary()))));
        if (!fractal.state().equals(fractal.input())) {
            blocks.add(new Block.State(List.of(new Block.StateVariable(fractal.state(), written(fractal.start()), false,
                    Optional.empty(), "", 0))));
        }
        blocks.addAll(stepped(fractal.state(), fractal.iteration()));
        blocks.add(outputs(output("escape", OutputType.IMAGE, Optional.empty(), List.of(FractalComputations.EscapeTime.FUNCTION
                + "(" + fractal.state() + ", " + DefinitionWords.BAILOUT + ", " + DefinitionWords.ITERATIONS + ")"))));
        return new Mapped(Kind.ITERATION, constants, Map.of(), blocks);
    }

    private static Mapped newtonFractal(NewtonFractal fractal) {
        List<Parameter> constants = List.of(
                constant(DefinitionWords.ITERATIONS, String.valueOf(fractal.maximumIterations()), ParameterFlag.INTEGER),
                constant(DefinitionWords.TOLERANCE, Notation.number(fractal.tolerance())));
        List<Block> blocks = new ArrayList<>();
        blocks.add(inputs(List.of(region(fractal.state(), fractal.real(), fractal.imaginary()))));
        blocks.addAll(stepped(fractal.state(), fractal.iteration()));
        blocks.add(outputs(output("basin", OutputType.IMAGE, Optional.empty(), List.of(FractalComputations.NewtonBasins.FUNCTION
                + "(" + fractal.state() + ", " + DefinitionWords.ITERATIONS + ", " + DefinitionWords.TOLERANCE + ")"))));
        return new Mapped(Kind.ITERATION, constants, Map.of(), blocks);
    }

    /** {@code formulas} for the step's local definitions, then {@code state_next = step}. */
    private static List<Block> stepped(String state, String iteration) {
        LocalDefinitions local = LocalDefinitions.of(iteration);
        List<Block> blocks = new ArrayList<>();
        formulas(local.definitions()).ifPresent(blocks::add);
        blocks.add(new Block.Equations(List.of(new Equation(EquationForm.NEXT, state, null, null, written(local.result())))));
        return blocks;
    }

    /** {@code (z) in -2 - 2 i .. 2 + 2 i}: a rectangle of the complex plane between two corners. */
    private static Block.Input region(String name, Interval real, Interval imaginary) {
        Range corners = new Range(written(Notation.complex(real.minimum(), imaginary.minimum())),
                written(Notation.complex(real.maximum(), imaginary.maximum())));
        return new Block.Input(List.of(name), Optional.of(corners), Optional.empty(), false, false, true, "");
    }

    // ─── fractals drawn by rules ────────────────────────────────────────────────────────────────────

    private static Mapped iteratedFunctionSystem(IteratedFunctionSystem system) {
        List<Block.WeightedMap> maps = new ArrayList<>();
        for (AffineMap map : system.maps()) {
            List<SourceText> rows = new ArrayList<>();
            for (int row = 0; row < system.dimension(); row++) {
                rows.add(written(AffineRows.write(map.matrix().get(row), map.translation().get(row),
                        AXES.subList(0, system.dimension()))));
            }
            maps.add(new Block.WeightedMap(written(Notation.number(map.weight())), rows));
        }
        return new Mapped(Kind.ITERATED_FUNCTION_SYSTEM, List.of(new Block.Maps(maps)));
    }

    private static Mapped lindenmayerSystem(LindenmayerSystem system) {
        Map<String, SourceText> turtle = new LinkedHashMap<>();
        turtle.put(DefinitionWords.ANGLE, written(Notation.number(system.angle()) + " deg"));
        turtle.put(DefinitionWords.GENERATIONS, written(String.valueOf(system.iterations())));
        return new Mapped(Kind.LINDENMAYER_SYSTEM, List.of(new Block.Axiom(system.axiom()),
                new Block.Rules(new TreeMap<>(system.rules())), new Block.Turtle(turtle)));
    }

    // ─── linear algebra ─────────────────────────────────────────────────────────────────────────────

    /** The entries as one output; the rows' labels, when they have them, in {@code meta}. */
    private static Mapped matrix(MatrixObject matrix) {
        Map<String, MetaValue> metaFields = new LinkedHashMap<>();
        if (!matrix.labels().isEmpty()) {
            metaFields.put(DefinitionWords.LABELS, new MetaValue.Items(matrix.labels().stream()
                    .<MetaValue>map(MetaValue.Text::new).toList()));
        }
        return new Mapped(Kind.MATRIX, List.of(), metaFields, List.of(outputs(output("M", OutputType.MATRIX,
                Optional.empty(), List.of(Notation.rows(matrix.entries()))))));
    }

    /**
     * A {@code mapping} with no inputs — it acts on the whole space — and {@code image : field = ((a) * x + (b) * y +
     * (t), …)}: every entry kept whole inside its parentheses.
     */
    private static Mapped affineTransformation(AffineTransformation transformation) {
        int dimension = transformation.matrix().size();
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < dimension; row++) {
            rows.add(AffineRows.expressionRow(transformation.matrix().get(row), transformation.translation().get(row),
                    AXES.subList(0, dimension)));
        }
        return new Mapped(Kind.MAPPING, List.of(outputs(output("image", OutputType.FIELD, Optional.empty(), rows))));
    }

    private static Mapped nonlinearTransformation(NonlinearTransformation transformation) {
        return new Mapped(Kind.MAPPING, List.of(inputs(axisInputs(transformation.bounds())),
                outputs(output("image", OutputType.FIELD, Optional.empty(), transformation.components()))));
    }

    // ─── geometry ───────────────────────────────────────────────────────────────────────────────────

    /** One point per line; an empty set says its dimension, which no point can. */
    private static Mapped pointSet(PointSet points) {
        List<List<SourceText>> written = points.points().stream()
                .map(point -> point.stream().map(coordinate -> written(Notation.number(coordinate))).toList()).toList();
        List<Parameter> constants = points.points().isEmpty()
                ? List.of(constant(DefinitionWords.DIMENSION, String.valueOf(points.dimension()), ParameterFlag.INTEGER))
                : List.of();
        return new Mapped(Kind.POINTS, constants, Map.of(), List.of(new Block.PointList(written)));
    }

    /**
     * {@code A = point(ax, 0) "A"}, {@code circle = circle_through_three(A, B, C) "circumcircle"}: the references
     * first, then the coordinates; the dimension is written only when the free points do not show it.
     */
    private static Mapped construction(GeometricConstruction construction) {
        List<SourceText> steps = new ArrayList<>();
        for (GeometricElement element : construction.elements()) {
            List<String> arguments = new ArrayList<>(element.references());
            arguments.addAll(element.coordinates());
            String label = element.label() == null ? "" : " " + Notation.text(element.label());
            steps.add(written(element.id() + " = " + DefinitionWords.word(element.type()) + "("
                    + String.join(", ", arguments) + ")" + label));
        }
        List<Parameter> constants = shownDimension(construction) == construction.dimension() ? List.of()
                : List.of(constant(DefinitionWords.DIMENSION, String.valueOf(construction.dimension()), ParameterFlag.INTEGER));
        return new Mapped(Kind.CONSTRUCTION, constants, Map.of(), List.of(new Block.Steps(steps)));
    }

    /** The dimension a construction's first free point shows by its coordinates; 2 when it has none. */
    static int shownDimension(GeometricConstruction construction) {
        return construction.elements().stream().filter(element -> element.type() == GeometricElementType.POINT)
                .findFirst().map(point -> point.coordinates().size()).orElse(2);
    }

    /** The vertex rule or the listed vertices, then the edge condition or the listed pairs; the dimension a constant. */
    private static Mapped polytope(Polytope polytope) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Vertices(written(polytope.vertexRule() != null ? polytope.vertexRule()
                : Notation.rows(polytope.vertices().stream().map(vertex -> vertex.stream().map(Notation::number).toList()).toList()))));
        if (polytope.edgeCondition() != null) {
            blocks.add(new Block.Edges(Optional.of(written(polytope.edgeCondition())), Optional.empty()));
        } else if (!polytope.edges().isEmpty()) {
            blocks.add(new Block.Edges(Optional.empty(), Optional.of(written(Notation.rows(polytope.edges().stream()
                    .map(edge -> edge.stream().map(String::valueOf).toList()).toList())))));
        }
        return new Mapped(Kind.POLYTOPE, List.of(constant(DefinitionWords.DIMENSION, String.valueOf(polytope.dimension()),
                ParameterFlag.INTEGER)), Map.of(), blocks);
    }

    /**
     * A {@code region}: the two inputs with their bounds as written, {@code integer} when they range over the
     * integers, the exact parameters as constants, and the condition as {@code picture : image where …}.
     */
    private static Mapped region(Region region) {
        List<Parameter> constants = new ArrayList<>();
        new TreeMap<>(region.exactIntegers()).forEach((name, digits) ->
                constants.add(constant(name, digits, ParameterFlag.INTEGER, ParameterFlag.EXACT)));
        LocalDefinitions local = LocalDefinitions.of(region.condition());
        List<Block.Input> inputs = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            inputs.add(new Block.Input(List.of(region.variables().get(index)), Optional.of(new Range(
                    written(region.from().get(index)), written(region.to().get(index)))), Optional.empty(),
                    region.integer(), false, false, ""));
        }
        List<Block> blocks = new ArrayList<>();
        blocks.add(inputs(inputs));
        formulas(local.definitions()).ifPresent(blocks::add);
        blocks.add(outputs(new Block.Output("picture", Optional.of(OutputType.IMAGE), Optional.empty(), List.of(),
                Optional.of(written(local.result())), "")));
        return new Mapped(Kind.REGION, constants, Map.of(), blocks);
    }

    // ─── probability ────────────────────────────────────────────────────────────────────────────────

    /** A curve over one variable or a surface over two, its one output of type {@code density}. */
    private static Mapped distribution(ProbabilityDistribution distribution) {
        List<Block.Input> inputs = new ArrayList<>();
        for (int index = 0; index < distribution.variables().size(); index++) {
            inputs.add(input(distribution.variables().get(index), distribution.support().get(index)));
        }
        return single(distribution.variables().size() == 1 ? Kind.CURVE : Kind.SURFACE, inputs, "density",
                OutputType.DENSITY, distribution.density());
    }

    // ─── sequences and programs ─────────────────────────────────────────────────────────────────────

    /** A curve over an integer input. */
    private static Mapped sequence(Sequence sequence) {
        Block.Input index = new Block.Input(List.of(sequence.variables().getFirst()), Optional.of(new Range(
                written(String.valueOf(sequence.first())), written(String.valueOf(sequence.last())))), Optional.empty(),
                true, false, false, "");
        return single(Kind.CURVE, List.of(index), "a", OutputType.SERIES, sequence.expression());
    }

    /** No inputs: one number, its output named as the measurement it is shown as. */
    private static Mapped value(Value value) {
        LocalDefinitions local = LocalDefinitions.of(value.expression());
        List<Block> blocks = new ArrayList<>();
        formulas(local.definitions()).ifPresent(blocks::add);
        blocks.add(outputs(output(value.name(), OutputType.SCALAR, Optional.empty(), List.of(local.result()))));
        return new Mapped(Kind.VALUE, blocks);
    }

    /**
     * Integer inputs become exact parameters, in the order of their names. The program is kept as its text: jMS reads
     * it where it runs, and a document carries no tree of it.
     */
    private static Mapped program(Program program) {
        List<Parameter> constants = new ArrayList<>();
        new TreeMap<>(program.integerInputs()).forEach((name, digits) ->
                constants.add(constant(name, digits, ParameterFlag.INTEGER, ParameterFlag.EXACT)));
        List<Block> blocks = new ArrayList<>();
        if (!program.formulas().isBlank()) {
            blocks.add(new Block.Formulas(Optional.of(written(program.formulas()))));
        }
        blocks.add(new Block.Run(null, written(program.program())));
        return new Mapped(Kind.PROGRAM, constants, Map.of(), blocks);
    }

    // ─── electronics ────────────────────────────────────────────────────────────────────────────────

    /** Parts in the one netlist notation, the sheet's boundary, its layout and probes, then the transient's span. */
    private static Mapped circuit(Circuit circuit) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Parts(circuit.elements().stream().map(DefinitionBlocks::part).toList()));
        if (!circuit.boundary().ports().isEmpty()) {
            blocks.add(new Block.Ports(circuit.boundary().ports().stream().map(DefinitionBlocks::port).toList()));
        }
        if (!circuit.boundary().globals().isEmpty()) {
            blocks.add(new Block.Nets(circuit.boundary().globals().stream()
                    .map(net -> new Block.Net(net, false, true, List.of())).toList()));
        }
        if (circuit.layout() != null) {
            blocks.add(new Block.Layout(circuit.layout().items().stream().map(DefinitionBlocks::layoutItem).toList(),
                    circuit.layout().wires().stream().map(DefinitionBlocks::wire).toList()));
        }
        if (!circuit.probes().isEmpty() || circuit.timePerDivision() != null) {
            blocks.add(new Block.Probes(circuit.probes().stream().map(DefinitionBlocks::probe).toList(),
                    Optional.ofNullable(circuit.timePerDivision()).map(DefinitionBlocks::timeBase)));
        }
        blocks.add(analysis(circuit.time()));
        return new Mapped(Kind.CIRCUIT, blocks);
    }

    /**
     * {@code R1 resistor (in, 0) resistance = R}, {@code V1 voltage-source (in, 0) U_output = U}; a model is connected
     * by pin name and its overrides are its settings: {@code U1 lm358@3 (inp = ref, out = out) gain = 2}.
     */
    static Block.Part part(CircuitElement element) {
        Map<String, SourceText> settings = new LinkedHashMap<>();
        String reference;
        List<String> positional = List.of();
        Map<String, String> named = Map.of();
        if (element.type() == CircuitElementType.MODEL) {
            reference = element.component();
            named = new TreeMap<>(element.pins());
            if (!element.value().isBlank()) {
                settings.put(DefinitionWords.MODEL_VALUE, written(element.value()));
            }
        } else {
            Primitive primitive = Primitive.of(element.type());
            reference = primitive.reference();
            positional = List.of(element.positive(), element.negative());
            settings.put(primitive.valueSetting(), written(element.value()));
        }
        new TreeMap<>(element.overrides()).forEach((name, value) -> settings.put(name, written(value)));
        return new Block.Part(element.id(), reference, Optional.empty(), positional, named, settings,
                Optional.ofNullable(element.symbol()), 0);
    }

    /** {@code input VIN}: a net a sheet hands to its parent, and which way it goes. */
    static Block.Port port(ObjectDefinition.SheetPort port) {
        return new Block.Port(Block.PortDirection.valueOf(port.direction().toUpperCase(Locale.ROOT)), port.net());
    }

    /** {@code R1 at 240, 200 rotate 90 mirror name at 0, -40}, {@code U4.B at …}, {@code GND1 ground at 600, 520}. */
    static Block.LayoutItem layoutItem(ObjectDefinition.PlacedItem item) {
        Map<String, Block.Point> labels = new LinkedHashMap<>();
        for (String label : List.of("name", "value", "readings")) {
            ObjectDefinition.LabelOffset offset = item.labels().get(label);
            if (offset != null) {
                labels.put(label, new Block.Point(offset.x(), offset.y()));
            }
        }
        return new Block.LayoutItem(item.id(), Optional.ofNullable(item.unit()), Optional.ofNullable(item.mark()),
                Optional.ofNullable(item.net()), new Block.Placement(item.x(), item.y(), item.rotation(), item.mirrored(),
                labels, item.shows()));
    }

    /** {@code W1 wire R1.negative to C1.positive via 240 120, 300 120}. */
    static Block.LayoutWire wire(ObjectDefinition.SchematicWire wire) {
        return new Block.LayoutWire(wire.id(), wire.from(), wire.to(),
                wire.bends().stream().map(bend -> new Block.Point(bend.x(), bend.y())).toList());
    }

    /** {@code CH1 out against ref 2 V/div}, {@code CH3 current R1}, {@code L1 level clock}. */
    static Block.Probe probe(ObjectDefinition.BenchProbe probe) {
        char measure = switch (probe.measure()) {
            case "current" -> 'I';
            case "power" -> 'P';
            case "level" -> 'L';
            default -> 'V';
        };
        // A current clamp's scale is amperes a division, a voltage probe's volts.
        Optional<Quantity> scale = Optional.ofNullable(probe.scale())
                .flatMap(volts -> Quantity.of(volts, measure == 'I' ? "A" : "V"));
        Optional<String> expression = Optional.ofNullable(probe.expression()).map(String::strip).filter(shown -> !shown.isEmpty());
        return new Block.Probe(probe.id(), measure, probe.target(), Optional.ofNullable(probe.reference()), scale,
                Optional.ofNullable(probe.mode()), expression);
    }

    /** {@code 5 ms}: the time base in the largest unit that keeps the figure at one or more. */
    static Quantity timeBase(double secondsPerDivision) {
        double value = secondsPerDivision;
        int unit = 0;
        while (value < 1 && unit < TIME_UNITS.size() - 1) {
            value *= 1000;
            unit++;
        }
        double rounded = Math.round(value * 1e6) / 1e6;
        return Quantity.of(rounded, TIME_UNITS.get(unit)).orElseThrow();
    }

    /** {@code analysis transient for 0.01 s}, or {@code from … to …} for a span that does not start at 0. */
    static Block.Analysis analysis(Interval time) {
        Map<String, SourceText> settings = new LinkedHashMap<>();
        if (time.minimum() == 0) {
            settings.put("for", written(Notation.number(time.maximum()) + " s"));
        } else {
            settings.put("from", written(Notation.number(time.minimum()) + " s"));
            settings.put("to", written(Notation.number(time.maximum()) + " s"));
        }
        return new Block.Analysis(Block.AnalysisKind.TRANSIENT, settings);
    }

    /**
     * A symbol's drawing, as the JSON its definition carries, back as the drawing the reader builds: its size, paths,
     * pins, labels and indicators, and a living drawing's inputs and elements.
     */
    static Block.Symbol symbol(String json) {
        SymbolDrawing drawing = SymbolDrawing.fromJson(json);
        return new Block.Symbol(List.of(), Map.of(), Optional.empty(), Map.of(), drawing.width(), drawing.height(),
                drawing.paths(),
                drawing.pins().stream().map(pin -> new Block.SymbolPin(pin.name(), pin.x(), pin.y(), pin.label())).toList(),
                drawing.labels().stream().map(label -> new Block.SymbolLabel(label.subject(), label.x(), label.y())).toList(),
                drawing.indicators().stream().map(indicator -> new Block.SymbolIndicator(indicator.name(), indicator.left(),
                        indicator.top(), indicator.right(), indicator.bottom())).toList(),
                drawing.upright(),
                drawing.inputs().stream().map(input -> new Block.SymbolInput(input.name(), input.low(), input.high(),
                        input.range().isEmpty() ? Optional.empty() : Optional.of(written(input.range())), input.picture())).toList(),
                drawing.elements().stream().map(DefinitionBlocks::element).toList(), Map.of());
    }

    private static Block.SymbolElement element(SymbolDrawing.Element element) {
        List<Block.SymbolProperty> properties = new ArrayList<>();
        element.properties().forEach((name, value) -> properties.add(property(name, value)));
        return new Block.SymbolElement(element.shape(), element.name(), element.live(), element.path(), element.geometry(),
                element.condition() == null ? null : property("when", element.condition()), properties, element.hidden(),
                element.children().stream().map(DefinitionBlocks::element).toList(), 0);
    }

    /** A property as written; its jMF reading is the reader's, which reads the written text again. */
    private static Block.SymbolProperty property(String name, SymbolDrawing.Value value) {
        return new Block.SymbolProperty(name, written(value.written()), List.of(), value.motion());
    }

    // ─── graphs ─────────────────────────────────────────────────────────────────────────────────────

    /** {@code formulas}, {@code clock}, {@code panes} (left out when it is the one plane a graph has by default), {@code lines}. */
    private static Mapped graph(ObjectDefinition.Graph graph) {
        List<Block> blocks = new ArrayList<>();
        if (!graph.formulas().isBlank()) {
            blocks.add(new Block.Formulas(Optional.of(written(graph.formulas()))));
        }
        if (graph.clock() != null) {
            ObjectDefinition.GraphClock clock = graph.clock();
            Optional<Range> range = clock.from() != null && clock.to() != null
                    ? Optional.of(new Range(written(clock.from()), written(clock.to()))) : Optional.empty();
            blocks.add(new Block.Clock(clock.parameter(), range, clock.speed(), clock.loop()));
        }
        boolean onePlane = graph.panes().size() == 1 && graph.panes().getFirst().name().equals("main")
                && graph.panes().getFirst().kind().equals("PLANE") && graph.panes().getFirst().axes().isEmpty();
        if (!onePlane) {
            blocks.add(new Block.Panes(Block.PaneLayout.valueOf(graph.layout().toUpperCase(Locale.ROOT)),
                    graph.panes().stream().map(DefinitionBlocks::pane).toList()));
        }
        blocks.add(new Block.Lines(graph.lines().stream().map(DefinitionBlocks::graphLine).toList()));
        for (ObjectDefinition.GraphCheck check : graph.checks()) {
            // The criterion travels as it was written: the reader reads it again.
            blocks.add(new Block.GraphCheck(check.title(), written(check.measure()), null, check.criterion(), 0));
        }
        return new Mapped(Kind.GRAPH, blocks);
    }

    private static Block.Pane pane(ObjectDefinition.GraphPane pane) {
        Map<String, Range> axes = new LinkedHashMap<>();
        pane.axes().forEach((axis, range) -> axes.put(axis, new Range(written(range.get(0)), written(range.get(1)))));
        Optional<Range> over = Optional.ofNullable(pane.over()).map(range -> new Range(written(range.get(0)), written(range.get(1))));
        return new Block.Pane(pane.name(), Block.PaneKind.valueOf(pane.kind().toUpperCase(Locale.ROOT)), axes,
                Optional.ofNullable(pane.uses()), over, Optional.ofNullable(pane.camera()), 0);
    }

    private static Block.GraphLine graphLine(ObjectDefinition.GraphLine line) {
        return new Block.GraphLine(line.name(), written(line.expression()), line.label(),
                Optional.ofNullable(line.until()).map(DefinitionBlocks::written),
                Optional.ofNullable(line.at()).map(DefinitionBlocks::written), Optional.ofNullable(line.from()),
                Optional.ofNullable(line.pane()), Optional.ofNullable(line.color()), line.hidden(), 0);
    }

    // ─── shared pieces ──────────────────────────────────────────────────────────────────────────────

    /**
     * A definition that is one expression over its inputs. Local definitions in front of the expression —
     * {@code r = sqrt(x^2 + y^2); sin(r) / r} — go in {@code formulas}, since {@code ;} ends a jMX line.
     */
    private static Mapped single(Kind kind, List<Block.Input> inputs, String output, OutputType type, String expression) {
        LocalDefinitions local = LocalDefinitions.of(expression);
        List<Block> blocks = new ArrayList<>();
        blocks.add(inputs(inputs));
        formulas(local.definitions()).ifPresent(blocks::add);
        blocks.add(outputs(output(output, type, Optional.empty(), List.of(local.result()))));
        return new Mapped(kind, blocks);
    }

    /** As {@link #single}, its output the set where the expression holds: {@code boundary : curve where …}. */
    private static Mapped implicit(Kind kind, List<Block.Input> inputs, OutputType type, String expression) {
        LocalDefinitions local = LocalDefinitions.of(expression);
        List<Block> blocks = new ArrayList<>();
        blocks.add(inputs(inputs));
        formulas(local.definitions()).ifPresent(blocks::add);
        blocks.add(outputs(new Block.Output("boundary", Optional.of(type), Optional.empty(), List.of(),
                Optional.of(written(local.result())), "")));
        return new Mapped(kind, blocks);
    }

    /** {@code formulas { … }} of definitions, a line each; nothing when there are none. */
    private static Optional<Block> formulas(List<String> definitions) {
        return definitions.isEmpty() ? Optional.empty()
                : Optional.of(new Block.Formulas(Optional.of(written(String.join("\n", definitions)))));
    }

    private static Block.Inputs inputs(List<Block.Input> inputs) {
        return new Block.Inputs(inputs);
    }

    private static Block.Outputs outputs(Block.Output output) {
        return new Block.Outputs(List.of(output));
    }

    /** {@code name : type [glued g] = components}: one expression, or a tuple of them. */
    private static Block.Output output(String name, OutputType type, Optional<String> gluing, List<String> components) {
        return new Block.Output(name, Optional.of(type), gluing, components.stream().map(DefinitionBlocks::written).toList(),
                Optional.empty(), "");
    }

    /** One input per axis of a box: {@code x in -2 .. 2}, {@code y in -2 .. 2}. */
    private static List<Block.Input> axisInputs(List<Interval> bounds) {
        List<Block.Input> inputs = new ArrayList<>();
        for (int index = 0; index < bounds.size(); index++) {
            inputs.add(input(AXES.get(index), bounds.get(index)));
        }
        return inputs;
    }

    private static Block.Input input(String name, Interval interval) {
        return new Block.Input(List.of(name), Optional.of(range(interval)), Optional.empty(), false, false, false, "");
    }

    private static Range range(Interval interval) {
        return new Range(written(Notation.number(interval.minimum())), written(Notation.number(interval.maximum())));
    }

    /** {@code bailout = 2}, {@code iterations = 100 integer}: a value of the definition, written as a parameter. */
    private static Parameter constant(String name, String value, ParameterFlag... flags) {
        Set<ParameterFlag> written = flags.length == 0 ? Set.of() : EnumSet.of(flags[0], flags);
        return new Parameter(name, written(value), Quantity.parse(value), Optional.empty(), Optional.empty(), written, "");
    }

    /** Text as a document holds what its author wrote; one built here was written by no one, so it stands nowhere. */
    static SourceText written(String text) {
        return new SourceText(text, 0, 0, 0);
    }
}
