package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.engine.DefinitionWords.Primitive;
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
import org.jmouse.parallax.fractal.FractalComputations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

import static org.jmouse.parallax.script.engine.DefinitionWords.AXES;

/**
 * Writes an {@link ObjectDefinition} as the kind word, the {@code type}, the constants and the blocks of a jMX
 * document, by the mapping table in {@code JMX-MAPPING.md}. Expressions are written exactly as the definition holds
 * them; only numbers the definition keeps as numbers are formatted, by {@link Notation#number}.
 *
 * <p>A {@link Component} is not written here: its source is the whole document, and {@link ExperienceDefinitions}
 * returns it as it is.</p>
 */
final class DefinitionWriter {

    private DefinitionWriter() {
    }

    static WrittenDefinition write(ObjectDefinition definition) {
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
            case Symbol symbol -> symbol(symbol);
            case ObjectDefinition.Graph graph -> graph(graph);
            case ObjectDefinition.Family family -> throw new IllegalArgumentException(
                    "A family is a line of a graph; it is kept as the graph's text, not as an object of its own.");
            case Component component -> throw new IllegalArgumentException(
                    "A component is written as its own source, not assembled from blocks.");
        };
    }

    // ─── curves and surfaces ────────────────────────────────────────────────────────────────────────

    private static WrittenDefinition explicitCurve(ExplicitCurve curve) {
        return single("curve", inputs(input(curve.variables().getFirst(), curve.x())), "y : curve = ", curve.expression());
    }

    private static WrittenDefinition parametricCurve(ParametricCurve curve) {
        String parameter = input(curve.variables().getFirst(), curve.t()) + (curve.closed() ? " periodic" : "");
        return plain("curve", inputs(parameter), outputs("trace : curve = " + tuple(curve.components())));
    }

    private static WrittenDefinition implicitCurve(ImplicitCurve curve) {
        List<String> names = curve.variables();
        return single("curve", inputs(input(names.get(0), curve.x()), input(names.get(1), curve.y())),
                "boundary : curve where ", curve.expression());
    }

    private static WrittenDefinition explicitSurface(ExplicitSurface surface) {
        List<String> names = surface.variables();
        return single("surface", inputs(input(names.get(0), surface.x()), input(names.get(1), surface.y())),
                "z : surface = ", surface.expression());
    }

    private static WrittenDefinition parametricSurface(ParametricSurface surface) {
        List<String> names = surface.variables();
        String gluing = surface.gluing() == SurfaceGluing.NONE ? "" : " glued " + DefinitionWords.word(surface.gluing());
        return plain("surface", inputs(input(names.get(0), surface.u()), input(names.get(1), surface.v())),
                outputs("shape : surface" + gluing + " = " + tuple(surface.components())));
    }

    private static WrittenDefinition implicitSurface(ImplicitSurface surface) {
        List<String> names = surface.variables();
        return single("surface", inputs(input(names.get(0), surface.x()), input(names.get(1), surface.y()),
                        input(names.get(2), surface.z())), "boundary : surface where ", surface.expression());
    }

    private static WrittenDefinition implicitHypersurface(ImplicitHypersurface hypersurface) {
        return single("hypersurface", inputs(input("x", hypersurface.x()), input("y", hypersurface.y()),
                        input("z", hypersurface.z()), input("w", hypersurface.w())),
                "boundary : volume where ", hypersurface.expression());
    }

    // ─── fields and dynamics ────────────────────────────────────────────────────────────────────────

    private static WrittenDefinition vectorField(VectorField field) {
        return plain("field", inputs(axisInputs(field.bounds())), outputs("flow : field = " + tuple(field.components())));
    }

    private static WrittenDefinition differentialSystem(DifferentialSystem system) {
        List<String> equations = new ArrayList<>();
        for (int index = 0; index < system.variables().size(); index++) {
            equations.add("d(" + system.variables().get(index) + ")/dt = " + system.equations().get(index));
        }
        return plain("dynamics", state(system.variables(), system.starts(), system.bounds()),
                TextBlock.braced("equations", equations), inputs(input("t", system.time())));
    }

    /** The step's values first, as {@code let} lines, then one update per variable. */
    private static WrittenDefinition discreteMap(DiscreteMap map) {
        List<String> equations = new ArrayList<>();
        map.stepValues().forEach(value -> equations.add("let " + value.name() + " = " + value.expression()));
        for (int index = 0; index < map.variables().size(); index++) {
            equations.add(map.variables().get(index) + "_next = " + map.updates().get(index));
        }
        return plain("iteration", state(map.variables(), map.starts(), map.bounds()), TextBlock.braced("equations", equations));
    }

    /** {@code x = 0 continuous in -25 .. 25}: where each variable starts, and its bounds when it has them. */
    private static TextBlock state(List<String> variables, List<String> starts, List<Interval> bounds) {
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < variables.size(); index++) {
            String line = variables.get(index) + " = " + starts.get(index) + " continuous";
            lines.add(bounds.isEmpty() ? line : line + " in " + range(bounds.get(index)));
        }
        return TextBlock.braced("state", lines);
    }

    // ─── the complex plane ──────────────────────────────────────────────────────────────────────────

    private static WrittenDefinition complexMapping(ComplexMapping mapping) {
        return single("mapping", inputs(region(mapping.variables().getFirst(), mapping.real(), mapping.imaginary())),
                "w : field = ", mapping.expression());
    }

    /**
     * An {@code iteration} whose output folds every point's orbit into a picture: the state starts where its
     * {@code state} line says, or at the point when the input is the state, and the engine's limits are constants.
     */
    private static WrittenDefinition escapeTimeFractal(EscapeTimeFractal fractal) {
        List<String> constants = List.of(DefinitionWords.BAILOUT + " = " + Notation.number(fractal.bailout()),
                DefinitionWords.ITERATIONS + " = " + fractal.maximumIterations() + " integer");
        List<TextBlock> blocks = new ArrayList<>();
        blocks.add(inputs(region(fractal.input(), fractal.real(), fractal.imaginary())));
        if (!fractal.state().equals(fractal.input())) {
            blocks.add(TextBlock.braced("state", List.of(fractal.state() + " = " + fractal.start() + " continuous")));
        }
        blocks.addAll(stepped(fractal.state(), fractal.iteration()));
        blocks.add(outputs("escape : image = " + FractalComputations.EscapeTime.FUNCTION + "(" + fractal.state() + ", "
                + DefinitionWords.BAILOUT + ", " + DefinitionWords.ITERATIONS + ")"));
        return new WrittenDefinition("iteration", Optional.empty(), constants, List.of(), blocks);
    }

    private static WrittenDefinition newtonFractal(NewtonFractal fractal) {
        List<String> constants = List.of(
                DefinitionWords.ITERATIONS + " = " + fractal.maximumIterations() + " integer",
                DefinitionWords.TOLERANCE + " = " + Notation.number(fractal.tolerance()));
        List<TextBlock> blocks = new ArrayList<>();
        blocks.add(inputs(region(fractal.state(), fractal.real(), fractal.imaginary())));
        blocks.addAll(stepped(fractal.state(), fractal.iteration()));
        blocks.add(outputs("basin : image = " + FractalComputations.NewtonBasins.FUNCTION + "(" + fractal.state() + ", "
                + DefinitionWords.ITERATIONS + ", " + DefinitionWords.TOLERANCE + ")"));
        return new WrittenDefinition("iteration", Optional.empty(), constants, List.of(), blocks);
    }

    /** {@code formulas} for the step's local definitions, then {@code state_next = step}. */
    private static List<TextBlock> stepped(String state, String iteration) {
        LocalDefinitions local = LocalDefinitions.of(iteration);
        List<TextBlock> blocks = new ArrayList<>();
        if (!local.definitions().isEmpty()) {
            blocks.add(TextBlock.braced("formulas", local.definitions()));
        }
        blocks.add(TextBlock.braced("equations", List.of(state + "_next = " + local.result())));
        return blocks;
    }

    /** {@code (z) in -2 - 2 i .. 2 + 2 i}: a rectangle of the complex plane between two corners. */
    private static String region(String name, Interval real, Interval imaginary) {
        return "(" + name + ") in " + Notation.complex(real.minimum(), imaginary.minimum()) + " .. "
                + Notation.complex(real.maximum(), imaginary.maximum());
    }

    // ─── fractals drawn by rules ────────────────────────────────────────────────────────────────────

    private static WrittenDefinition iteratedFunctionSystem(IteratedFunctionSystem system) {
        List<String> maps = new ArrayList<>();
        for (AffineMap map : system.maps()) {
            List<String> rows = new ArrayList<>();
            for (int row = 0; row < system.dimension(); row++) {
                rows.add(AffineRows.write(map.matrix().get(row), map.translation().get(row), AXES.subList(0, system.dimension())));
            }
            maps.add(Notation.number(map.weight()) + " : (" + String.join(", ", rows) + ")");
        }
        return plain("iterated_function_system", TextBlock.braced("maps", maps));
    }

    private static WrittenDefinition lindenmayerSystem(LindenmayerSystem system) {
        List<String> rules = new ArrayList<>();
        new TreeMap<>(system.rules()).forEach((symbol, replacement) -> rules.add(symbol + " -> " + Notation.text(replacement)));
        return plain("lindenmayer_system", TextBlock.statement("axiom " + Notation.text(system.axiom())),
                TextBlock.braced("rules", rules),
                TextBlock.braced("turtle", List.of(DefinitionWords.ANGLE + " = " + Notation.number(system.angle()) + " deg",
                        DefinitionWords.GENERATIONS + " = " + system.iterations())));
    }

    // ─── linear algebra ─────────────────────────────────────────────────────────────────────────────

    /** The entries as one output; the rows' labels, when they have them, in {@code meta}. */
    private static WrittenDefinition matrix(MatrixObject matrix) {
        List<String> metaFields = matrix.labels().isEmpty() ? List.of() : List.of(DefinitionWords.LABELS + " ["
                + String.join(", ", matrix.labels().stream().map(Notation::text).toList()) + "]");
        return new WrittenDefinition("matrix", Optional.empty(), List.of(), metaFields,
                List.of(outputs("M : matrix = " + Notation.rows(matrix.entries()))));
    }

    /**
     * A {@code mapping} with no inputs — it acts on the whole space — and {@code image : field = ((a) * x + (b) * y +
     * (t), …)}: every entry kept whole inside its parentheses.
     */
    private static WrittenDefinition affineTransformation(AffineTransformation transformation) {
        int dimension = transformation.matrix().size();
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < dimension; row++) {
            rows.add(AffineRows.expressionRow(transformation.matrix().get(row), transformation.translation().get(row),
                    AXES.subList(0, dimension)));
        }
        return plain("mapping", outputs("image : field = (" + String.join(", ", rows) + ")"));
    }

    private static WrittenDefinition nonlinearTransformation(NonlinearTransformation transformation) {
        return plain("mapping", inputs(axisInputs(transformation.bounds())),
                outputs("image : field = " + tuple(transformation.components())));
    }

    // ─── geometry ───────────────────────────────────────────────────────────────────────────────────

    /** One point per line; an empty set says its dimension, which no point can. */
    private static WrittenDefinition pointSet(PointSet points) {
        List<String> lines = points.points().stream()
                .map(point -> "(" + String.join(", ", point.stream().map(Notation::number).toList()) + ")").toList();
        List<String> constants = points.points().isEmpty()
                ? List.of(DefinitionWords.DIMENSION + " = " + points.dimension() + " integer") : List.of();
        return new WrittenDefinition("points", Optional.empty(), constants, List.of(),
                List.of(TextBlock.braced("points", lines)));
    }

    /**
     * {@code A = point(ax, 0) "A"}, {@code circle = circle_through_three(A, B, C) "circumcircle"}: the references
     * first, then the coordinates; the dimension is written only when the free points do not show it.
     */
    private static WrittenDefinition construction(GeometricConstruction construction) {
        List<String> steps = new ArrayList<>();
        for (GeometricElement element : construction.elements()) {
            List<String> arguments = new ArrayList<>(element.references());
            arguments.addAll(element.coordinates());
            String label = element.label() == null ? "" : " " + Notation.text(element.label());
            steps.add(element.id() + " = " + DefinitionWords.word(element.type()) + "(" + String.join(", ", arguments) + ")" + label);
        }
        List<String> constants = shownDimension(construction) == construction.dimension()
                ? List.of() : List.of(DefinitionWords.DIMENSION + " = " + construction.dimension() + " integer");
        return new WrittenDefinition("construction", Optional.empty(), constants, List.of(),
                List.of(TextBlock.braced("steps", steps)));
    }

    /** The dimension a construction's first free point shows by its coordinates; 2 when it has none. */
    static int shownDimension(GeometricConstruction construction) {
        return construction.elements().stream().filter(element -> element.type() == GeometricElementType.POINT)
                .findFirst().map(point -> point.coordinates().size()).orElse(2);
    }

    /** The vertex rule or the listed vertices, then the edge condition or the listed pairs; the dimension a constant. */
    private static WrittenDefinition polytope(Polytope polytope) {
        List<TextBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock.statement("vertices = " + (polytope.vertexRule() != null ? polytope.vertexRule()
                : Notation.rows(polytope.vertices().stream().map(vertex -> vertex.stream().map(Notation::number).toList()).toList()))));
        if (polytope.edgeCondition() != null) {
            blocks.add(TextBlock.statement("edges where " + polytope.edgeCondition()));
        } else if (!polytope.edges().isEmpty()) {
            blocks.add(TextBlock.statement("edges = " + Notation.rows(polytope.edges().stream()
                    .map(edge -> edge.stream().map(String::valueOf).toList()).toList())));
        }
        return new WrittenDefinition("polytope", Optional.empty(),
                List.of(DefinitionWords.DIMENSION + " = " + polytope.dimension() + " integer"), List.of(), blocks);
    }

    /**
     * A {@code region}: the two inputs with their bounds as written, {@code integer} when they range over the
     * integers, the exact parameters as constants, and the condition as {@code picture : image where …}.
     */
    private static WrittenDefinition region(Region region) {
        String integer = region.integer() ? " integer" : "";
        List<String> constants = new ArrayList<>();
        new TreeMap<>(region.exactIntegers()).forEach((name, digits) -> constants.add(name + " = " + digits + " integer exact"));
        LocalDefinitions local = LocalDefinitions.of(region.condition());
        List<TextBlock> blocks = new ArrayList<>();
        blocks.add(inputs(region.variables().get(0) + " in " + region.from().get(0) + " .. " + region.to().get(0) + integer,
                region.variables().get(1) + " in " + region.from().get(1) + " .. " + region.to().get(1) + integer));
        if (!local.definitions().isEmpty()) {
            blocks.add(TextBlock.braced("formulas", local.definitions()));
        }
        blocks.add(outputs("picture : image where " + local.result()));
        return new WrittenDefinition("region", Optional.empty(), constants, List.of(), blocks);
    }

    // ─── probability ────────────────────────────────────────────────────────────────────────────────

    /** A curve over one variable or a surface over two, its one output of type {@code density}. */
    private static WrittenDefinition distribution(ProbabilityDistribution distribution) {
        List<String> inputs = new ArrayList<>();
        for (int index = 0; index < distribution.variables().size(); index++) {
            inputs.add(input(distribution.variables().get(index), distribution.support().get(index)));
        }
        return single(distribution.variables().size() == 1 ? "curve" : "surface", inputs(inputs), "density : density = ",
                distribution.density());
    }

    // ─── sequences and programs ─────────────────────────────────────────────────────────────────────

    /** A curve over an integer input. */
    private static WrittenDefinition sequence(Sequence sequence) {
        return single("curve", inputs(sequence.variables().getFirst() + " in " + sequence.first() + " .. "
                + sequence.last() + " integer"), "a : series = ", sequence.expression());
    }

    /** No inputs: one number, its output named as the measurement it is shown as. */
    private static WrittenDefinition value(Value value) {
        LocalDefinitions local = LocalDefinitions.of(value.expression());
        List<TextBlock> blocks = new ArrayList<>();
        if (!local.definitions().isEmpty()) {
            blocks.add(TextBlock.braced("formulas", local.definitions()));
        }
        blocks.add(outputs(value.name() + " : scalar = " + local.result()));
        return new WrittenDefinition("value", Optional.empty(), List.of(), List.of(), blocks);
    }

    /** Integer inputs become exact parameters, in the order of their names. */
    private static WrittenDefinition program(Program program) {
        List<String> constants = new ArrayList<>();
        new TreeMap<>(program.integerInputs()).forEach((name, digits) -> constants.add(name + " = " + digits + " integer exact"));
        List<TextBlock> blocks = new ArrayList<>();
        if (!program.formulas().isBlank()) {
            blocks.add(TextBlock.braced("formulas", List.of(program.formulas())));
        }
        blocks.add(TextBlock.braced("run", List.of(program.program())));
        return new WrittenDefinition("program", Optional.empty(), constants, List.of(), blocks);
    }

    // ─── electronics ────────────────────────────────────────────────────────────────────────────────

    /** Parts in the one netlist notation, then the span of the transient response. */
    private static WrittenDefinition circuit(Circuit circuit) {
        List<String> parts = circuit.elements().stream().map(DefinitionWriter::part).toList();
        String analysis = analysis(circuit.time());
        List<TextBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock.braced("parts", parts));
        if (!circuit.boundary().ports().isEmpty()) {
            blocks.add(TextBlock.braced("ports", circuit.boundary().ports().stream().map(DefinitionWriter::port).toList()));
        }
        if (!circuit.boundary().globals().isEmpty()) {
            blocks.add(TextBlock.braced("nets", circuit.boundary().globals().stream().map(net -> net + " global").toList()));
        }
        if (circuit.layout() != null) {
            blocks.add(TextBlock.braced("layout", layoutLines(circuit.layout())));
        }
        if (!circuit.probes().isEmpty() || circuit.timePerDivision() != null) {
            List<String> probes = new ArrayList<>(circuit.probes().stream().map(DefinitionWriter::probe).toList());
            if (circuit.timePerDivision() != null) {
                probes.add(timeBase(circuit.timePerDivision()));
            }
            blocks.add(TextBlock.braced("probes", probes));
        }
        blocks.add(TextBlock.statement(analysis));
        return plain("circuit", blocks.toArray(TextBlock[]::new));
    }

    /** {@code CH1 out against ref 2 V/div}, {@code M2 current PS1.negative rms}, {@code M3 power RL}. */
    /** The word of the {@code probes} line that sets the oscilloscope's time base. */
    static final String TIME_BASE = "time";

    /** {@code time 5 ms/div}: the time base in the largest unit that keeps the figure at one or more. */
    static String timeBase(double secondsPerDivision) {
        String[] units = {"s", "ms", "µs", "ns"};
        double value = secondsPerDivision;
        int unit = 0;
        while (value < 1 && unit < units.length - 1) {
            value *= 1000;
            unit++;
        }
        return TIME_BASE + " " + Notation.number(Math.round(value * 1e6) / 1e6) + " " + units[unit] + "/div";
    }

    static String probe(ObjectDefinition.BenchProbe probe) {
        StringBuilder line = new StringBuilder(probe.id());
        if (!probe.measure().equals("voltage")) {
            line.append(' ').append(probe.measure());
        }
        line.append(' ').append(probe.target());
        if (probe.reference() != null) {
            line.append(" against ").append(probe.reference());
        }
        if (probe.scale() != null) {
            // A current clamp's scale is amperes a division, a voltage probe's volts.
            line.append(' ').append(Notation.number(probe.scale())).append(probe.measure().equals("current") ? " A/div" : " V/div");
        }
        if (probe.mode() != null) {
            line.append(' ').append(probe.mode());
        }
        if (probe.expression() != null && !probe.expression().isBlank()) {
            line.append(" shows ").append(probe.expression().strip());
        }
        return line.toString();
    }

    /** {@code analysis transient for 0.01 s}, or {@code from … to …} for a span that does not start at 0. */
    static String analysis(Interval time) {
        return time.minimum() == 0
                ? "analysis transient for " + Notation.number(time.maximum()) + " s"
                : "analysis transient from " + Notation.number(time.minimum()) + " s to " + Notation.number(time.maximum()) + " s";
    }

    /** The {@code layout} block's lines: the placed items in order, then the wires. */
    static List<String> layoutLines(ObjectDefinition.SchematicLayout layout) {
        List<String> lines = new ArrayList<>();
        layout.items().forEach(item -> lines.add(placed(item)));
        layout.wires().forEach(wire -> lines.add(wire(wire)));
        return lines;
    }

    /** {@code R1 at 240, 200 rotate 90 mirror name at 0, -40}, {@code GND1 ground at 600, 520}. */
    static String placed(ObjectDefinition.PlacedItem item) {
        StringBuilder line = new StringBuilder(item.placement());
        if (item.mark() != null) {
            line.append(' ').append(item.mark());
        }
        if (item.net() != null) {
            line.append(' ').append(item.net());
        }
        line.append(" at ").append(Notation.number(item.x())).append(", ").append(Notation.number(item.y()));
        if (item.rotation() != 0) {
            line.append(" rotate ").append(Notation.number(item.rotation()));
        }
        if (item.mirrored()) {
            line.append(" mirror");
        }
        for (String label : List.of("name", "value", "readings")) {
            ObjectDefinition.LabelOffset offset = item.labels().get(label);
            if (offset != null) {
                line.append(' ').append(label).append(" at ").append(Notation.number(offset.x())).append(", ").append(Notation.number(offset.y()));
            }
        }
        if (!item.shows().isEmpty()) {
            line.append(" shows ").append(String.join(" ", item.shows()));
        }
        return line.toString();
    }

    /** {@code input VIN}: a net a sheet hands to its parent, and which way it goes. */
    static String port(ObjectDefinition.SheetPort port) {
        return port.direction() + " " + port.net();
    }

    /** {@code vcc global}, {@code agnd ground route 300,200 380,200}: a {@code nets} line as the reader takes it back. */
    static String net(org.jmouse.parallax.script.Block.Net net) {
        StringBuilder line = new StringBuilder(net.net());
        if (net.ground()) {
            line.append(" ground");
        }
        if (net.global()) {
            line.append(" global");
        }
        if (!net.route().isEmpty()) {
            line.append(" route ").append(String.join(" ", net.route().stream()
                    .map(point -> Notation.number(point.x()) + "," + Notation.number(point.y())).toList()));
        }
        return line.toString();
    }

    /** {@code W1 wire R1.negative to C1.positive via 240 120, 300 120}. */
    static String wire(ObjectDefinition.SchematicWire wire) {
        String via = wire.bends().isEmpty() ? "" : " via " + String.join(", ", wire.bends().stream()
                .map(bend -> Notation.number(bend.x()) + " " + Notation.number(bend.y())).toList());
        return wire.id() + " wire " + wire.from() + " to " + wire.to() + via;
    }

    /**
     * {@code R1 resistor (in, 0) resistance = R}, {@code V1 voltage-source (in, 0) U_output = U};
     * a model is connected by pin name and its overrides are its settings:
     * {@code U1 lm358@3 (inp = ref, out = out) gain = 2}.
     */
    static String part(CircuitElement element) {
        List<String> settings = new ArrayList<>();
        String connections;
        String reference;
        if (element.type() == CircuitElementType.MODEL) {
            reference = element.component();
            connections = String.join(", ", new TreeMap<>(element.pins()).entrySet().stream()
                    .map(pin -> pin.getKey() + " = " + pin.getValue()).toList());
            if (!element.value().isBlank()) {
                settings.add(DefinitionWords.MODEL_VALUE + " = " + element.value());
            }
        } else {
            Primitive primitive = Primitive.of(element.type());
            reference = primitive.reference();
            connections = element.positive() + ", " + element.negative();
            settings.add(primitive.valueSetting() + " = " + element.value());
        }
        new TreeMap<>(element.overrides()).forEach((name, value) -> settings.add(name + " = " + value));
        String symbol = element.symbol() == null ? "" : " symbol " + element.symbol();
        return (element.id() + " " + reference + " (" + connections + ") " + String.join(", ", settings)).strip() + symbol;
    }

    /** The drawing as the lines of a {@code symbol} document: its size, its paths, its pins, its labels. */
    private static WrittenDefinition symbol(Symbol symbol) {
        List<TextBlock> lines = new ArrayList<>(drawingLines(symbol.drawing()).stream().map(TextBlock::statement).toList());
        lines.addAll(livingBlocks(symbol.drawing()));
        return new WrittenDefinition("symbol", Optional.empty(), List.of(), List.of(), lines);
    }

    /**
     * A living drawing's {@code input} lines and elements, after its plain lines. Kept apart from {@link #drawingLines}:
     * those are the lines a canvas redraws, and these it leaves as written.
     */
    static List<TextBlock> livingBlocks(String json) {
        SymbolDrawing drawing = SymbolDrawing.fromJson(json);
        List<TextBlock> blocks = new ArrayList<>();
        drawing.inputs().forEach(input -> blocks.add(TextBlock.statement("input " + input.name()
                + (input.range().isEmpty() ? "" : " " + input.range()))));
        drawing.elements().forEach(element -> blocks.add(elementBlock(element)));
        return blocks;
    }

    private static TextBlock elementBlock(SymbolDrawing.Element element) {
        StringBuilder opening = new StringBuilder(element.live() ? "live " : "").append(element.shape());
        if (element.condition() != null) {
            opening.append(' ').append(element.condition().written());
        }
        if (!element.name().isEmpty()) {
            opening.append(' ').append(element.name());
        }
        if (!element.path().isEmpty()) {
            opening.append(' ').append(Notation.text(element.path()));
        }
        element.geometry().forEach((word, numbers) -> opening.append(' ').append(word).append(' ').append(String.join(
                word.equals("size") ? " x " : ", ", numbers.stream().map(Notation::number).toList())));
        List<String> lines = new ArrayList<>();
        element.properties().forEach((name, value) -> lines.add(switch (value.motion()) {
            case LIVE -> "live ";
            case ANIMATED -> "animate ";
            case STILL -> "";
        } + name + " = " + value.written()));
        if (!element.hidden().isEmpty()) {
            lines.add("hide " + String.join(", ", element.hidden()));
        }
        element.children().forEach(child -> lines.add(elementBlock(child).render("").stripTrailing()));
        return lines.isEmpty() ? TextBlock.statement(opening.toString()) : TextBlock.braced(opening.toString(), lines);
    }

    /** A drawing as its lines, in the order a symbol document writes them: size, paths, pins, labels. */
    static List<String> drawingLines(String json) {
        SymbolDrawing drawing = SymbolDrawing.fromJson(json);
        List<String> lines = new ArrayList<>();
        if (drawing.width() > 0 || drawing.height() > 0) {
            lines.add("size " + Notation.number(drawing.width()) + " x " + Notation.number(drawing.height()));
        }
        drawing.paths().forEach(path -> lines.add("path " + Notation.text(path)));
        drawing.upright().forEach(path -> lines.add("path " + Notation.text(path) + " upright"));
        drawing.pins().forEach(pin -> lines.add("pin " + pin.name() + " at " + Notation.number(pin.x()) + ", "
                + Notation.number(pin.y()) + (pin.label().isEmpty() ? "" : " " + Notation.text(pin.label()))));
        drawing.labels().forEach(label -> lines.add("label " + label.subject() + " at " + Notation.number(label.x())
                + ", " + Notation.number(label.y())));
        drawing.indicators().forEach(indicator -> lines.add("indicator " + indicator.name() + " from "
                + Notation.number(indicator.left()) + ", " + Notation.number(indicator.top()) + " to "
                + Notation.number(indicator.right()) + ", " + Notation.number(indicator.bottom())));
        return lines;
    }

    // ─── graphs ─────────────────────────────────────────────────────────────────────────────────────

    /** {@code formulas}, {@code clock}, {@code panes} (left out when it is the one plane a graph has by default), {@code lines}. */
    private static WrittenDefinition graph(ObjectDefinition.Graph graph) {
        List<TextBlock> blocks = new ArrayList<>();
        if (!graph.formulas().isBlank()) {
            blocks.add(TextBlock.braced("formulas", List.of(graph.formulas())));
        }
        if (graph.clock() != null) {
            ObjectDefinition.GraphClock clock = graph.clock();
            String range = clock.from() != null && clock.to() != null ? " in " + clock.from() + " .. " + clock.to() : "";
            String speed = clock.speed() == 1 ? "" : " speed " + Notation.number(clock.speed());
            blocks.add(TextBlock.statement("clock " + clock.parameter() + range + speed + (clock.loop() ? " loop" : "")));
        }
        boolean onePlane = graph.panes().size() == 1 && graph.panes().getFirst().name().equals("main")
                && graph.panes().getFirst().kind().equals("PLANE") && graph.panes().getFirst().axes().isEmpty();
        if (!onePlane) {
            List<String> panes = graph.panes().stream().map(DefinitionWriter::paneLine).toList();
            blocks.add(TextBlock.braced("panes " + graph.layout().toLowerCase(Locale.ROOT), panes));
        }
        blocks.add(TextBlock.braced("lines", graph.lines().stream().map(DefinitionWriter::graphLine).toList()));
        for (ObjectDefinition.GraphCheck check : graph.checks()) {
            blocks.add(TextBlock.braced("expect " + Notation.text(check.title()), List.of("measure " + check.measure(), check.criterion())));
        }
        return new WrittenDefinition("graph", Optional.empty(), List.of(), List.of(), blocks);
    }

    private static String paneLine(ObjectDefinition.GraphPane pane) {
        StringBuilder line = new StringBuilder(pane.name()).append(' ').append(pane.kind().toLowerCase(Locale.ROOT));
        pane.axes().forEach((axis, range) -> line.append("  ").append(axis).append(' ').append(range.get(0))
                .append(" .. ").append(range.get(1)));
        if (pane.uses() != null) {
            line.append(" uses ").append(pane.uses());
        }
        if (pane.over() != null) {
            line.append(" over ").append(pane.over().get(0)).append(" .. ").append(pane.over().get(1));
        }
        if (pane.camera() != null) {
            line.append(" camera ").append(pane.camera());
        }
        return line.toString();
    }

    private static String graphLine(ObjectDefinition.GraphLine line) {
        StringBuilder written = new StringBuilder(line.name()).append("  ")
                .append(line.label() ? Notation.text(line.expression()) : line.expression());
        if (line.until() != null) {
            written.append("  until ").append(line.until());
        }
        if (line.at() != null) {
            written.append("  at ").append(line.at());
        }
        if (line.from() != null) {
            written.append("  from ").append(line.from());
        }
        if (line.pane() != null) {
            written.append("  in ").append(line.pane());
        }
        if (line.color() != null) {
            written.append("  color ").append(line.color());
        }
        if (line.hidden()) {
            written.append("  hidden");
        }
        return written.toString();
    }

    // ─── shared pieces ──────────────────────────────────────────────────────────────────────────────

    private static WrittenDefinition plain(String kind, TextBlock... blocks) {
        return new WrittenDefinition(kind, Optional.empty(), List.of(), List.of(), List.of(blocks));
    }

    /**
     * A definition that is one expression over its inputs. Local definitions in front of the expression —
     * {@code r = sqrt(x^2 + y^2); sin(r) / r} — go in {@code formulas}, since {@code ;} ends a jMX line.
     *
     * @param output the output's head, up to the expression: {@code "z : surface = "}
     */
    private static WrittenDefinition single(String kind, TextBlock inputs, String output, String expression) {
        LocalDefinitions local = LocalDefinitions.of(expression);
        List<TextBlock> blocks = new ArrayList<>();
        blocks.add(inputs);
        if (!local.definitions().isEmpty()) {
            blocks.add(TextBlock.braced("formulas", local.definitions()));
        }
        blocks.add(outputs(output + local.result()));
        return new WrittenDefinition(kind, Optional.empty(), List.of(), List.of(), blocks);
    }

    private static TextBlock inputs(String... inputs) {
        return TextBlock.braced("inputs", List.of(inputs));
    }

    private static TextBlock inputs(List<String> inputs) {
        return TextBlock.braced("inputs", inputs);
    }

    private static TextBlock outputs(String output) {
        return TextBlock.braced("outputs", List.of(output));
    }

    /** One input per axis of a box: {@code x in -2 .. 2}, {@code y in -2 .. 2}. */
    private static List<String> axisInputs(List<Interval> bounds) {
        List<String> inputs = new ArrayList<>();
        for (int index = 0; index < bounds.size(); index++) {
            inputs.add(input(AXES.get(index), bounds.get(index)));
        }
        return inputs;
    }

    private static String input(String name, Interval interval) {
        return name + " in " + range(interval);
    }

    private static String range(Interval interval) {
        return Notation.number(interval.minimum()) + " .. " + Notation.number(interval.maximum());
    }

    private static String tuple(List<String> components) {
        return "(" + String.join(", ", components) + ")";
    }
}
