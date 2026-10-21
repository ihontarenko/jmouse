package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.Amount;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Equation;
import org.jmouse.parallax.script.Block.EquationForm;
import org.jmouse.parallax.script.Block.Input;
import org.jmouse.parallax.script.Block.Output;
import org.jmouse.parallax.script.Block.Part;
import org.jmouse.parallax.script.Block.StateVariable;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.dimension.PolytopeSkeleton;
import org.jmouse.parallax.fractal.FractalComputations;
import org.jmouse.parallax.script.engine.DefinitionWords.Primitive;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.AffineMap;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.object.ObjectDefinition.GeometricElement;
import org.jmouse.parallax.object.ObjectDefinition.GeometricElementType;
import org.jmouse.parallax.object.ObjectDefinition.SurfaceGluing;
import org.jmouse.units.Quantity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import static org.jmouse.parallax.script.engine.DefinitionWords.AXES;

/**
 * The engine a jMX document is computed by, and what that engine is told — the one place where a document's
 * structure becomes an {@link ObjectDefinition}. A kind is structure; the engine is <em>derived</em> from it, never
 * named by the author:
 *
 * <table>
 *   <tr><th>Kind</th><th>Structure</th><th>Engine</th></tr>
 *   <tr><td>{@code curve}</td><td>an output of type {@code density}</td><td>density: samples, cumulative, moments</td></tr>
 *   <tr><td></td><td>{@code where} over two inputs</td><td>implicit curve</td></tr>
 *   <tr><td></td><td>a tuple over one input</td><td>parametric curve</td></tr>
 *   <tr><td></td><td>one expression over one {@code integer} input</td><td>sequence: terms, sums, ratios</td></tr>
 *   <tr><td></td><td>one expression over one input</td><td>explicit curve</td></tr>
 *   <tr><td>{@code surface}</td><td>an output of type {@code density} over two inputs</td><td>density of two variables</td></tr>
 *   <tr><td></td><td>{@code where} over three inputs · a tuple over two · one expression over two</td>
 *       <td>implicit · parametric · explicit surface</td></tr>
 *   <tr><td>{@code region}</td><td>{@code where} over two inputs · integer inputs with {@code exact} parameters</td>
 *       <td>the pixels where the condition holds · decided in exact fractions</td></tr>
 *   <tr><td>{@code mapping}</td><td>one complex input {@code (z)}</td><td>complex mapping</td></tr>
 *   <tr><td></td><td>real inputs, one per axis, and a tuple</td><td>non-linear map of the plane or space</td></tr>
 *   <tr><td></td><td>no inputs, a tuple {@code ((a) * x + (b) * y + (t), …)}</td><td>affine map</td></tr>
 *   <tr><td>{@code iteration}</td><td>an output calling {@code escape_time(…)}</td><td>escape time over the plane</td></tr>
 *   <tr><td></td><td>an output calling {@code newton_basin(…)}</td><td>basins of settling over the plane</td></tr>
 *   <tr><td></td><td>a {@code state} and {@code x_next}</td><td>discrete map; sample paths when a step calls a
 *       {@code random_…} function</td></tr>
 *   <tr><td>{@code dynamics}</td><td>a {@code state} and {@code d(x)/dt}</td><td>differential system; Euler–Maruyama
 *       paths when an equation calls {@code white_noise()}</td></tr>
 *   <tr><td>{@code matrix}</td><td>one matrix output</td><td>matrix; a Markov chain's evolution when its rows are
 *       distributions</td></tr>
 *   <tr><td>{@code polytope}</td><td>{@code vertices = rule(…)}, {@code edges where …}</td><td>the polytope engine</td></tr>
 *   <tr><td>{@code value}</td><td>no inputs, one expression</td><td>evaluation</td></tr>
 * </table>
 *
 * <p>Every other kind maps to one engine. A function an engine supplies — {@code escape_time}, {@code newton_basin},
 * {@code cartesian}, {@code hamming}, {@code random_sign}, {@code white_noise} — is declared by that engine and recognised
 * on the expression's tree, as a host call, never by matching text.
 * Inputs keep the names their author gave them: a curve over {@code θ} is computed over {@code θ}.</p>
 *
 * <p>A component is not read here: its definition is its source text, which a document no longer has, so
 * {@link ExperienceDefinitions} reads it from the text.</p>
 */
public final class EngineSelection {

    private static final Pattern STEP = Pattern.compile("([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*([a-z_]+)\\s*\\((.*)\\)\\s*(?:([\"'])(.*)\\4)?\\s*");

    /** The functions engines supply to an output, read as host calls so they are found on the tree. */
    private static final FormulaLanguage ENGINE_CALLS = FormulaLanguage.standard()
            .withHostCalls(FractalComputations.EscapeTime.FUNCTION, FractalComputations.NewtonBasins.FUNCTION);

    private final ExperienceDocument  document;
    private final DefinitionConstants constants;
    private final String              what;

    private EngineSelection(ExperienceDocument document, DefinitionConstants constants) {
        this.document = document;
        this.constants = constants;
        this.what = document.kind().keyword() + " '" + document.slug() + "'";
    }

    /**
     * The definition a document computes, its rangeless parameters taken as the engine's constants.
     *
     * @throws IllegalArgumentException when the structure is not one an engine computes, naming what is missing
     */
    public static ObjectDefinition of(ExperienceDocument document) {
        List<Block.Parameter> rangeless = document.parameters().stream()
                .filter(parameter -> parameter.range().isEmpty()).toList();
        return read(document, new DefinitionConstants(rangeless));
    }

    static ObjectDefinition read(ExperienceDocument document, DefinitionConstants constants) {
        EngineSelection selection = new EngineSelection(document, constants);
        ObjectDefinition definition = selection.definition();
        constants.requireAllTaken();
        return definition;
    }

    private ObjectDefinition definition() {
        if (document.type().isPresent()) {
            throw new IllegalArgumentException(what + " says 'type " + document.type().get() + "', but its engine "
                    + "follows from its structure: a family is written as its formula, not named.");
        }
        return switch (document.kind()) {
            case CURVE -> curve();
            case SURFACE -> surface();
            case HYPERSURFACE -> new ObjectDefinition.ImplicitHypersurface(withLocalDefinitions(implicitExpression()), interval("x"),
                    interval("y"), interval("z"), interval("w"));
            case FIELD -> vectorField();
            case DYNAMICS -> differentialSystem();
            case ITERATION -> iteration();
            case MAPPING -> mapping();
            case REGION -> region();
            case VALUE -> value();
            case ITERATED_FUNCTION_SYSTEM -> iteratedFunctionSystem();
            case LINDENMAYER_SYSTEM -> lindenmayerSystem();
            case MATRIX -> new ObjectDefinition.MatrixObject(Notation.rows(singleOutput(), what), labels());
            case POINTS -> pointSet();
            case CONSTRUCTION -> construction();
            case POLYTOPE -> polytope();
            case PROGRAM -> program();
            case CIRCUIT -> circuit();
            case SYMBOL -> symbol();
            case GRAPH -> graph();
            case COMPONENT -> throw new IllegalArgumentException(what + " is its own source: read it from its text.");
            case FAMILY, FUNCTIONS, CATEGORY, COLLECTION, COURSE, INSTRUMENT -> throw new IllegalArgumentException(
                    what + " is not a catalogue object: a " + document.kind().keyword() + " defines nothing to compute.");
        };
    }

    // ─── curves and surfaces ────────────────────────────────────────────────────────────────────────

    /** A density, implicit, parametric, over integers, explicit — by how the output is written and what the input is. */
    private ObjectDefinition curve() {
        Output output = output();
        if (isDensity(output)) {
            return distribution(1);
        }
        if (output.equation().isPresent()) {
            List<String> variables = inputNames(2);
            return new ObjectDefinition.ImplicitCurve(withLocalDefinitions(output.equation().get().text()),
                    interval(variables.get(0)), interval(variables.get(1)), variables);
        }
        Input input = onlyInput();
        String variable = input.names().getFirst();
        if (output.components().size() > 1) {
            return new ObjectDefinition.ParametricCurve(texts(output.components()), interval(variable), input.periodic(),
                    List.of(variable));
        }
        if (input.integer()) {
            Block.Range range = range(input, variable);
            return new ObjectDefinition.Sequence(withLocalDefinitions(singleOutput()),
                    Notation.parseInteger(range.minimum().text(), "the first term of " + what),
                    Notation.parseInteger(range.maximum().text(), "the last term of " + what), List.of(variable));
        }
        return new ObjectDefinition.ExplicitCurve(withLocalDefinitions(singleOutput()), interval(variable), List.of(variable));
    }

    private ObjectDefinition surface() {
        Output output = output();
        if (isDensity(output)) {
            return distribution(2);
        }
        if (output.equation().isPresent()) {
            List<String> variables = inputNames(3);
            return new ObjectDefinition.ImplicitSurface(withLocalDefinitions(output.equation().get().text()),
                    interval(variables.get(0)), interval(variables.get(1)), interval(variables.get(2)), variables);
        }
        List<String> variables = inputNames(2);
        if (output.components().size() > 1) {
            SurfaceGluing gluing = output.gluing()
                    .map(word -> DefinitionWords.constant(SurfaceGluing.class, word, "a gluing")).orElse(SurfaceGluing.NONE);
            return new ObjectDefinition.ParametricSurface(texts(output.components()), interval(variables.get(0)),
                    interval(variables.get(1)), gluing, variables);
        }
        return new ObjectDefinition.ExplicitSurface(withLocalDefinitions(singleOutput()), interval(variables.get(0)),
                interval(variables.get(1)), variables);
    }

    // ─── fields and dynamics ────────────────────────────────────────────────────────────────────────

    private ObjectDefinition vectorField() {
        List<String> components = tupleOutput();
        return new ObjectDefinition.VectorField(components, axisIntervals(components.size()));
    }

    private ObjectDefinition differentialSystem() {
        List<String> variables = stateNames();
        List<String> equations = new ArrayList<>();
        for (String variable : variables) {
            equations.add(equation(EquationForm.DERIVATIVE, variable));
        }
        return new ObjectDefinition.DifferentialSystem(variables, equations, interval("t"), stateBounds(), starts(),
                document.block(Block.Image.class).map(image -> flowImage(image, variables)).orElse(null));
    }

    /** The {@code image} block, its starts in the order of the state; a variable without a start is refused. */
    private ObjectDefinition.FlowImage flowImage(Block.Image image, List<String> variables) {
        List<String> starts = new ArrayList<>();
        for (String variable : variables) {
            SourceText start = image.starts().get(variable);
            if (start == null) {
                throw new IllegalArgumentException(what + ": the image starts every state variable — 'start " + variable + " = …' is missing.");
            }
            starts.add(start.text());
        }
        for (String named : image.starts().keySet()) {
            if (!variables.contains(named)) {
                throw new IllegalArgumentException(what + ": the image starts '" + named + "', which is not in the state " + variables + ".");
            }
        }
        return new ObjectDefinition.FlowImage(image.horizontal(), imageInterval(image.horizontalRange()), image.vertical(),
                imageInterval(image.verticalRange()), starts, image.stops().stream().map(SourceText::text).toList(),
                image.crossing().text(), image.where().map(SourceText::text).orElse(null), image.value().text());
    }

    private Interval imageInterval(Block.Range range) {
        String of = "the image range of " + what;
        return new Interval(constantValue(range.minimum().text(), of), constantValue(range.maximum().text(), of));
    }

    /** An expression of numbers alone — {@code 2 * pi}, {@code 1 / 3} — evaluated, refused when it is not one. */
    private static double constantValue(String expression, String of) {
        try {
            return FormulaLanguage.standard().evaluate(expression, Map.of());
        } catch (FormulaException unreadable) {
            throw new IllegalArgumentException(of + " is a number or an expression of numbers, not '" + expression
                    + "': " + unreadable.getMessage(), unreadable);
        }
    }

    /** A checked amount in SI: a quantity as written ({@code 6.86 kHz}), or an expression of numbers. */
    private double amountValue(Amount amount) {
        return amount.quantity().map(Quantity::value)
                .orElseGet(() -> constantValue(amount.expression().text(), "an expected value of " + what));
    }

    /**
     * A state stepped by {@code x_next}: folded into a picture of the plane when an output calls an engine's
     * function, otherwise iterated as a discrete map.
     */
    private ObjectDefinition iteration() {
        for (Output output : document.outputs()) {
            Optional<Formula.HostCall> call = engineCall(output);
            if (call.isPresent()) {
                return planeIteration(call.get());
            }
        }
        List<String> variables = stateNames();
        List<String> updates = new ArrayList<>();
        for (String variable : variables) {
            updates.add(equation(EquationForm.NEXT, variable));
        }
        List<ObjectDefinition.StepValue> stepValues = equations().stream()
                .filter(equation -> equation.form() == EquationForm.LOCAL)
                .map(equation -> new ObjectDefinition.StepValue(equation.subject(), equation.expression().text())).toList();
        return new ObjectDefinition.DiscreteMap(variables, updates, stateBounds(), starts(), stepValues);
    }

    /** Where each state variable starts, as written: {@code x = 0 continuous}. */
    private List<String> starts() {
        return stateVariables().stream().map(variable -> variable.initial().text()).toList();
    }

    /** The engine function an output is written as, when it is one: {@code escape_time(z, bailout, iterations)}. */
    private Optional<Formula.HostCall> engineCall(Output output) {
        if (output.components().size() != 1) {
            return Optional.empty();
        }
        Formula expression;
        try {
            expression = ENGINE_CALLS.parse(output.components().getFirst().text());
        } catch (FormulaException unreadable) {
            return Optional.empty();
        }
        return expression instanceof Formula.HostCall call ? Optional.of(call) : Optional.empty();
    }

    /**
     * {@code escape_time(state, bailout, iterations)} or {@code newton_basin(state, iterations, tolerance)}: the first
     * argument names the state the equations step, the others are numbers or rangeless parameters.
     */
    private ObjectDefinition planeIteration(Formula.HostCall call) {
        List<Formula> arguments = call.arguments();
        if (arguments.size() != 3 || !(arguments.getFirst() instanceof Formula.Variable(String state))) {
            throw new IllegalArgumentException(what + " calls " + call.name() + "(state, …, …) with the state it steps "
                    + "first and two numbers after it.");
        }
        Input input = onlyInput();
        if (!input.complex()) {
            throw new IllegalArgumentException(what + " steps its state from every point of a complex input: "
                    + "'(c) in -2 - 2 i .. 2 + 2 i'.");
        }
        String point = input.names().getFirst();
        Interval[] region = region(input);
        String step = withLocalDefinitions(equation(EquationForm.NEXT, state));
        if (call.name().equals(FractalComputations.NewtonBasins.FUNCTION)) {
            if (!state.equals(point)) {
                throw new IllegalArgumentException(what + " settles from every point, so its input is the state '"
                        + state + "' itself: '(" + state + ") in …'.");
            }
            return new ObjectDefinition.NewtonFractal(state, step, wholeNumber(arguments.get(1)), number(arguments.get(2)),
                    region[0], region[1]);
        }
        return new ObjectDefinition.EscapeTimeFractal(state, stateStart(state, point), step, point,
                number(arguments.get(1)), wholeNumber(arguments.get(2)), region[0], region[1]);
    }

    /** Where the state starts: its {@code state} line, or the point itself when the input is the state. */
    private String stateStart(String state, String point) {
        Optional<StateVariable> declared = document.block(Block.State.class).stream()
                .flatMap(block -> block.variables().stream()).filter(variable -> variable.name().equals(state)).findFirst();
        if (declared.isPresent()) {
            return declared.get().initial().text();
        }
        if (!state.equals(point)) {
            throw new IllegalArgumentException(what + " says where '" + state + "' starts: 'state { " + state
                    + " = 0 continuous }', or makes it the input '(" + state + ") in …'.");
        }
        return point;
    }

    /** A number an engine is told: written in place, or the name of a parameter written without a range. */
    private double number(Formula argument) {
        return switch (argument) {
            case Formula.Constant constant -> constant.value();
            case Formula.Negation(Formula.Constant constant) -> -constant.value();
            case Formula.Variable(String name) -> constants.number(name, what);
            default -> throw new IllegalArgumentException(what + " passes an engine a number or a parameter written "
                    + "without a range, not an expression.");
        };
    }

    private int wholeNumber(Formula argument) {
        double value = number(argument);
        if (!Notation.isInteger(value)) {
            throw new IllegalArgumentException(what + " counts iterations in whole numbers, not " + value + ".");
        }
        return (int) value;
    }

    private List<StateVariable> stateVariables() {
        return document.block(Block.State.class).map(Block.State::variables)
                .orElseThrow(() -> new IllegalArgumentException(what + " has a 'state' block naming its variables."));
    }

    private List<String> stateNames() {
        return stateVariables().stream().map(StateVariable::name).toList();
    }

    /** Every state variable's range, or none when no variable has one; some with and some without is refused. */
    private List<Interval> stateBounds() {
        List<StateVariable> variables = stateVariables();
        long bounded = variables.stream().filter(variable -> variable.range().isPresent()).count();
        if (bounded == 0) {
            return List.of();
        }
        if (bounded != variables.size()) {
            throw new IllegalArgumentException(what + " gives every state variable a range, or none.");
        }
        return variables.stream().map(variable -> interval(variable.range().orElseThrow(), variable.name())).toList();
    }

    /** The right side of the one equation of a form for a variable: {@code d(x)/dt = …} or {@code x_next = …}. */
    private String equation(EquationForm form, String variable) {
        return equations().stream().filter(equation -> equation.form() == form && variable.equals(equation.subject()))
                .findFirst().map(equation -> equation.expression().text())
                .orElseThrow(() -> new IllegalArgumentException(what + " has no " + (form == EquationForm.DERIVATIVE
                        ? "d(" + variable + ")/dt" : variable + "_next") + " equation."));
    }

    private List<Equation> equations() {
        return document.block(Block.Equations.class).map(Block.Equations::equations).orElse(List.of());
    }

    // ─── mappings ───────────────────────────────────────────────────────────────────────────────────

    /** A complex input is a complex mapping; real axes map the plane or space; no inputs at all, an affine map. */
    private ObjectDefinition mapping() {
        List<Input> inputs = inputList();
        if (inputs.size() == 1 && inputs.getFirst().complex()) {
            Input input = inputs.getFirst();
            Interval[] region = region(input);
            return new ObjectDefinition.ComplexMapping(withLocalDefinitions(singleOutput()), region[0], region[1],
                    List.of(input.names().getFirst()));
        }
        if (inputs.isEmpty()) {
            return affineTransformation();
        }
        List<String> components = tupleOutput();
        return new ObjectDefinition.NonlinearTransformation(components, axisIntervals(components.size()));
    }

    /** The real and imaginary intervals of a complex input: {@code (z) in -2 - 2 i .. 2 + 2 i}. */
    private Interval[] region(Input input) {
        Block.Range range = input.range().orElseThrow(() -> new IllegalArgumentException(
                what + " writes the corners of its complex input: '(z) in -2 - 2 i .. 2 + 2 i'."));
        double[] lower = Notation.parseComplex(range.minimum().text(), "the lower corner of " + what);
        double[] upper = Notation.parseComplex(range.maximum().text(), "the upper corner of " + what);
        return new Interval[] {new Interval(lower[0], upper[0]), new Interval(lower[1], upper[1])};
    }

    /** {@code ((a) * x + (b) * y + (t), …)}: each parenthesised group is an entry, the last one the translation. */
    private ObjectDefinition affineTransformation() {
        List<String> rows = tupleOutput();
        List<List<String>> matrix = new ArrayList<>();
        List<String> translation = new ArrayList<>();
        for (String row : rows) {
            List<String> groups = parenthesisedGroups(row);
            if (groups.size() != rows.size() + 1 || !AffineRows.expressionRow(groups.subList(0, rows.size()), groups.getLast(),
                    AXES.subList(0, rows.size())).equals(row)) {
                throw new IllegalArgumentException(what + " has no inputs, so it maps the whole space linearly: each "
                        + "row is written '(a) * x + (b) * y + (t)', not '" + row + "'. A map of a bounded region "
                        + "names its inputs.");
            }
            matrix.add(groups.subList(0, groups.size() - 1));
            translation.add(groups.getLast());
        }
        return new ObjectDefinition.AffineTransformation(matrix, translation);
    }

    /** The contents of the parenthesised groups at the top level of a text, in order. */
    private static List<String> parenthesisedGroups(String row) {
        List<String> groups = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int index = 0; index < row.length(); index++) {
            char character = row.charAt(index);
            if (character == '(') {
                if (depth == 0) {
                    start = index + 1;
                }
                depth++;
            } else if (character == ')') {
                depth--;
                if (depth == 0) {
                    groups.add(row.substring(start, index).strip());
                }
            }
        }
        return groups;
    }

    // ─── one number ─────────────────────────────────────────────────────────────────────────────────

    private ObjectDefinition value() {
        if (!inputList().isEmpty()) {
            throw new IllegalArgumentException(what + " is one number, so it has no inputs; an expression over an "
                    + "input is a curve.");
        }
        Output output = output();
        return new ObjectDefinition.Value(output.name(), withLocalDefinitions(singleOutput()));
    }

    // ─── fractals drawn by rules ────────────────────────────────────────────────────────────────────

    private ObjectDefinition iteratedFunctionSystem() {
        List<Block.WeightedMap> written = document.block(Block.Maps.class).map(Block.Maps::maps)
                .filter(writtenMaps -> !writtenMaps.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException(what + " has a 'maps' block with at least one map."));
        int dimension = written.getFirst().components().size();
        if (dimension < 1 || dimension > AXES.size()) {
            throw new IllegalArgumentException(what + " maps a space of 1 to " + AXES.size() + " axes, not " + dimension + ".");
        }
        List<String> variables = AXES.subList(0, dimension);
        List<AffineMap> maps = new ArrayList<>();
        for (Block.WeightedMap map : written) {
            if (map.components().size() != dimension) {
                throw new IllegalArgumentException(what + ": every map has " + dimension + " rows, one per axis, not "
                        + map.components().size() + ".");
            }
            List<List<Double>> matrix = new ArrayList<>();
            List<Double> translation = new ArrayList<>();
            for (SourceText row : map.components()) {
                double[] coefficients = AffineRows.read(row.text(), variables);
                matrix.add(Arrays.stream(coefficients, 0, dimension).boxed().toList());
                translation.add(coefficients[dimension]);
            }
            maps.add(new AffineMap(matrix, translation, Notation.parseNumber(map.weight().text(), "a weight of " + what)));
        }
        return new ObjectDefinition.IteratedFunctionSystem(dimension, maps);
    }

    private ObjectDefinition lindenmayerSystem() {
        String axiom = document.block(Block.Axiom.class).map(Block.Axiom::axiom)
                .orElseThrow(() -> new IllegalArgumentException(what + " has an 'axiom \"…\"'."));
        Map<String, String> rules = document.block(Block.Rules.class).map(Block.Rules::rules).orElse(Map.of());
        Map<String, SourceText> turtle = document.block(Block.Turtle.class).map(Block.Turtle::settings).orElse(Map.of());
        SourceText angle = turtle.get(DefinitionWords.ANGLE);
        SourceText generations = turtle.get(DefinitionWords.GENERATIONS);
        if (angle == null || generations == null) {
            throw new IllegalArgumentException(what + " writes 'turtle { angle = … deg;  generations = … }'.");
        }
        return new ObjectDefinition.LindenmayerSystem(axiom, rules, Notation.parseDegrees(angle.text(), "the angle of " + what),
                Notation.parseInteger(generations.text(), "the generations of " + what));
    }

    // ─── geometry ───────────────────────────────────────────────────────────────────────────────────

    private ObjectDefinition pointSet() {
        List<List<SourceText>> written = document.block(Block.PointList.class).map(Block.PointList::points).orElse(List.of());
        List<List<Double>> points = new ArrayList<>();
        for (List<SourceText> point : written) {
            List<String> coordinates = point.size() == 1
                    ? Notation.items(point.getFirst().text(), '(', ')', "a point of " + what) : texts(point);
            points.add(coordinates.stream().map(coordinate -> Notation.parseNumber(coordinate, "a coordinate of " + what)).toList());
        }
        int dimension = points.isEmpty() ? constants.integer(DefinitionWords.DIMENSION, what) : points.getFirst().size();
        return new ObjectDefinition.PointSet(dimension, points);
    }

    /** Each step is {@code id = type(arguments) ["label"]}; an argument naming an element is a reference. */
    private ObjectDefinition construction() {
        List<SourceText> steps = document.block(Block.Steps.class).map(Block.Steps::steps)
                .orElseThrow(() -> new IllegalArgumentException(what + " has a 'steps' block."));
        Set<String> identifiers = new HashSet<>();
        List<Matcher> matched = new ArrayList<>();
        for (SourceText step : steps) {
            Matcher matcher = STEP.matcher(step.text());
            if (!matcher.matches()) {
                throw new IllegalArgumentException(what + ": a step is 'id = type(arguments) [\"label\"]', not '" + step.text() + "'.");
            }
            identifiers.add(matcher.group(1));
            matched.add(matcher);
        }
        List<GeometricElement> elements = new ArrayList<>();
        for (Matcher step : matched) {
            GeometricElementType type = DefinitionWords.constant(GeometricElementType.class, step.group(2), "a construction step");
            List<String> references = new ArrayList<>();
            List<String> coordinates = new ArrayList<>();
            for (String argument : Notation.splitAtCommas(step.group(3))) {
                if (type != GeometricElementType.POINT && identifiers.contains(argument)) {
                    references.add(argument);
                } else {
                    coordinates.add(argument);
                }
            }
            elements.add(new GeometricElement(step.group(1), type, references, coordinates, step.group(5)));
        }
        ObjectDefinition.GeometricConstruction shown = new ObjectDefinition.GeometricConstruction(2, elements);
        int dimension = constants.optionalInteger(DefinitionWords.DIMENSION, what)
                .orElse(DefinitionBlocks.shownDimension(shown));
        return new ObjectDefinition.GeometricConstruction(dimension, elements);
    }

    /**
     * {@code vertices = cartesian(-1, 1, dimension)} or a list of points; {@code edges where hamming(first, second) = 1}
     * or a list of index pairs, or neither — then the shortest distances are the edges. The rule and the condition are
     * kept as written for the polytope engine; {@code dimension} is the document's constant, or a listed point's size.
     */
    private ObjectDefinition polytope() {
        String written = document.block(Block.Vertices.class).map(vertices -> vertices.expression().text())
                .orElseThrow(() -> new IllegalArgumentException(what + " writes its vertices: 'vertices = cartesian(-1, 1, "
                        + "dimension)' or 'vertices = [[0, 0], [1, 0], …]'."));
        boolean listed = written.strip().startsWith("[");
        List<List<Double>> vertices = listed ? numberRows(written, "the vertices of " + what) : List.of();
        String rule = listed ? null : written;
        if (rule != null && !isHostCall(rule)) {
            throw new IllegalArgumentException(what + " makes its vertices by a rule — cartesian(values…, count), "
                    + "axis_points(radius, count), regular_simplex(count) — or lists them, not '" + rule + "'.");
        }
        Optional<Block.Edges> edges = document.block(Block.Edges.class);
        String condition = edges.flatMap(Block.Edges::condition).map(SourceText::text).orElse(null);
        List<List<Integer>> pairs = edges.flatMap(Block.Edges::pairs)
                .map(listedPairs -> Notation.rows(listedPairs.text(), "the edges of " + what).stream()
                        .map(edge -> edge.stream().map(index -> Notation.parseInteger(index, "an edge of " + what)).toList())
                        .toList())
                .orElse(List.of());
        int dimension = listed && !vertices.isEmpty()
                ? constants.optionalInteger(DefinitionWords.DIMENSION, what).orElse(vertices.getFirst().size())
                : constants.integer(DefinitionWords.DIMENSION, what);
        return new ObjectDefinition.Polytope(dimension, rule, vertices, condition, pairs);
    }

    /** Whether a vertex rule is one call of the polytope engine; an unreadable rule is not. */
    private static boolean isHostCall(String rule) {
        try {
            return PolytopeSkeleton.LANGUAGE.parse(rule) instanceof Formula.HostCall;
        } catch (FormulaException unreadable) {
            return false;
        }
    }

    // ─── regions ────────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code picture : image where …} over two inputs, horizontal first: integers one pixel each when the inputs are
     * {@code integer}, decided exactly when the document has {@code exact} parameters. A bound may be an expression of
     * the parameters ({@code y in k + 16 .. k}); a range written downwards turns its axis over.
     */
    private ObjectDefinition region() {
        Output output = output();
        String condition = output.equation().map(SourceText::text).orElseThrow(() -> new IllegalArgumentException(
                what + " is written 'picture : image where condition'."));
        List<Input> inputs = inputList();
        List<String> variables = inputNames(2);
        boolean integer = inputs.stream().allMatch(Input::integer);
        if (!integer && inputs.stream().anyMatch(Input::integer)) {
            throw new IllegalArgumentException(what + " has two integer inputs or two real ones, not one of each.");
        }
        List<String> from = new ArrayList<>();
        List<String> to = new ArrayList<>();
        for (String variable : variables) {
            Block.Range range = range(input(variable), variable);
            from.add(range.minimum().text());
            to.add(range.maximum().text());
        }
        return new ObjectDefinition.Region(withLocalDefinitions(condition), variables, from, to, integer,
                constants.exactIntegers());
    }

    // ─── probability ────────────────────────────────────────────────────────────────────────────────

    private static boolean isDensity(Output output) {
        return output.type().filter(type -> type == Block.OutputType.DENSITY).isPresent();
    }

    /**
     * A density over its inputs, each a variable over its support. The cumulative function and the moments are not
     * written: the engine derives them from the density, which therefore has to be the only output.
     */
    private ObjectDefinition distribution(int variablesCount) {
        if (document.outputs().size() != 1) {
            throw new IllegalArgumentException(what + " is a density, so it has the one output 'density : density = …'; "
                    + "its samples, cumulative function and moments follow from it.");
        }
        List<String> variables = inputNames(variablesCount);
        List<Interval> support = variables.stream().map(this::interval).toList();
        return new ObjectDefinition.ProbabilityDistribution(variables, withLocalDefinitions(singleOutput()), support);
    }

    /** A matrix's row labels — the states of a chain — from {@code meta { labels ["Sunny", "Rain"] }}. */
    private List<String> labels() {
        return document.meta().field(DefinitionWords.LABELS).map(EngineSelection::texts).orElse(List.of());
    }

    /** The meta fields a definition owns, which are therefore not properties of the object. */
    static Set<String> ownedMetaFields(ExperienceDocument document) {
        return document.kind() == Kind.MATRIX ? Set.of(DefinitionWords.LABELS) : Set.of();
    }

    private static List<String> texts(Meta.MetaValue value) {
        return switch (value) {
            case Meta.MetaValue.Items items -> items.items().stream().map(ObjectDocumentReader::text).toList();
            case Meta.MetaValue.Text text -> List.of(text.text());
            case Meta.MetaValue.Names names -> names.names();
            default -> throw new IllegalArgumentException("Labels are written as a list: 'labels [\"Sunny\", \"Rain\"]'.");
        };
    }

    // ─── programs ───────────────────────────────────────────────────────────────────────────────────

    private ObjectDefinition program() {
        String program = document.block(Block.Run.class).map(run -> Notation.bodyWithoutIndentation(run.source().text()))
                .orElseThrow(() -> new IllegalArgumentException(what + " has a 'run { … }' block."));
        return new ObjectDefinition.Program(formulas().orElse(""), program, constants.exactIntegers());
    }

    /**
     * A one-expression definition with the local definitions of {@code formulas} in front of it, as jMF writes them
     * on one line: {@code r = sqrt(x^2 + y^2); sin(r) / r}.
     */
    private String withLocalDefinitions(String result) {
        return formulas().map(definitions -> LocalDefinitions.joined(definitions, result)).orElse(result);
    }

    /** The jMF definitions between the braces of {@code formulas}, without the block's indentation. */
    private Optional<String> formulas() {
        return document.block(Block.Formulas.class).flatMap(Block.Formulas::definitions)
                .map(definitions -> Notation.bodyWithoutIndentation(definitions.text()));
    }

    // ─── electronics ────────────────────────────────────────────────────────────────────────────────

    private ObjectDefinition circuit() {
        List<Part> parts = document.block(Block.Parts.class).map(Block.Parts::parts)
                .orElseThrow(() -> new IllegalArgumentException(what + " has a 'parts' block."));
        List<CircuitElement> elements = parts.stream().map(this::element).toList();
        Optional<Block.Probes> probes = document.block(Block.Probes.class);
        return new ObjectDefinition.Circuit(elements, transientSpan(),
                document.block(Block.Layout.class).map(EngineSelection::layout).orElse(null), null,
                probes.map(EngineSelection::probes).orElse(List.of()), circuitExpectations(),
                probes.flatMap(Block.Probes::timePerDivision).map(Quantity::value).orElse(null), sheetBoundary());
    }

    /** What crosses the circuit's edge as a sheet of a project: its {@code ports}, and its {@code nets} marked global. */
    private ObjectDefinition.SheetBoundary sheetBoundary() {
        List<ObjectDefinition.SheetPort> ports = document.block(Block.Ports.class).map(Block.Ports::ports).orElse(List.of())
                .stream().map(port -> new ObjectDefinition.SheetPort(port.net(), port.direction().name().toLowerCase(Locale.ROOT)))
                .toList();
        Set<String> globals = document.block(Block.Nets.class).map(Block.Nets::nets).orElse(List.of()).stream()
                .filter(Block.Net::global).map(Block.Net::net).collect(Collectors.toCollection(LinkedHashSet::new));
        return new ObjectDefinition.SheetBoundary(ports, globals);
    }

    /**
     * What the circuit states of itself — its {@code expect} blocks —, carried on the definition so Model checks run
     * them. They were read and then dropped here: a circuit's expectations were documentation nobody checked.
     */
    private List<ObjectDefinition.CircuitExpectation> circuitExpectations() {
        return document.blocks(Block.Expectation.class).stream().map(expectation -> {
            Block.Criterion criterion = expectation.criterion();
            if (criterion.kind() == Block.CriterionKind.IS) {
                return new ObjectDefinition.CircuitExpectation(expectation.title(), expectation.reference(), expectation.measure(),
                        "is", Double.NaN, Double.NaN, criterion.exact());
            }
            double[] bounds = criterion.bounds(this::amountValue);
            return new ObjectDefinition.CircuitExpectation(expectation.title(), expectation.reference(), expectation.measure(),
                    criterion.kind().name().toLowerCase(Locale.ROOT), bounds[0], bounds[1], null);
        }).toList();
    }

    /** The {@code probes} block as the definition carries it to the bench. */
    private static List<ObjectDefinition.BenchProbe> probes(Block.Probes probes) {
        return probes.probes().stream().map(probe -> new ObjectDefinition.BenchProbe(probe.id(), switch (probe.measure()) {
            case 'I' -> "current";
            case 'P' -> "power";
            case 'L' -> "level";
            default -> "voltage";
        }, probe.target(), probe.reference().orElse(null), probe.scale().map(Quantity::value).orElse(null),
                probe.mode().orElse(null), probe.expression().orElse(null))).toList();
    }

    private static Map<String, ObjectDefinition.LabelOffset> labelOffsets(Block.Placement placement) {
        Map<String, ObjectDefinition.LabelOffset> offsets = new LinkedHashMap<>();
        placement.labels().forEach((label, point) -> offsets.put(label, new ObjectDefinition.LabelOffset(point.x(), point.y())));
        return offsets;
    }

    /** The {@code layout} block as the definition carries it to the canvas that draws it. */
    private static ObjectDefinition.SchematicLayout layout(Block.Layout layout) {
        return new ObjectDefinition.SchematicLayout(
                layout.items().stream().map(item -> new ObjectDefinition.PlacedItem(item.id(), item.mark().orElse(null),
                        item.net().orElse(null),
                        item.placement().x(), item.placement().y(), item.placement().rotation(),
                        item.placement().mirrored(), labelOffsets(item.placement()), item.placement().shows(),
                        item.unit().orElse(null))).toList(),
                layout.wires().stream().map(wire -> new ObjectDefinition.SchematicWire(wire.id(), wire.from(), wire.to(),
                        wire.bends().stream().map(bend -> new ObjectDefinition.Bend(bend.x(), bend.y())).toList()))
                        .toList());
    }

    private CircuitElement element(Part part) {
        return element(part, what);
    }

    /**
     * A part line as every reader of one takes it — a circuit, and a component's datasheet bench alike: connected in
     * pin order it is a two-terminal primitive, connected by pin name a model; every setting is read in SI.
     *
     * @param what the document it is written in, for the messages
     */
    static CircuitElement element(Part part, String what) {
        Map<String, String> settings = new LinkedHashMap<>();
        part.settings().forEach((name, value) -> settings.put(name, inSi(value.text())));
        String frequency = settings.remove(DefinitionWords.FREQUENCY);
        // `()` on what is no primitive connects nothing: a sheet placed as a block that its global nets join.
        boolean unconnected = part.positional().isEmpty() && Primitive.ofReference(part.reference()).isEmpty();
        if (!part.named().isEmpty() || unconnected) {
            String value = settings.remove(DefinitionWords.MODEL_VALUE);
            // A model's frequency is one of its parameters (a generator's), not a primitive source's field.
            if (frequency != null) {
                settings.put(DefinitionWords.FREQUENCY, frequency);
            }
            return new CircuitElement(part.id(), CircuitElementType.MODEL, null, null, value,
                    part.reference(), part.named(), settings, part.symbol().orElse(null));
        }
        Primitive primitive = Primitive.ofReference(part.reference()).orElseThrow(() -> new IllegalArgumentException(
                what + ": part '" + part.id() + "' is connected in pin order, so it is one of " + String.join(", ",
                        Arrays.stream(Primitive.values()).map(Primitive::reference).toList())
                        + "; a model is connected by pin name '(pin = net, …)'."));
        if (part.positional().size() != 2) {
            throw new IllegalArgumentException(what + ": part '" + part.id() + "' joins two nodes: '(positive, negative)'.");
        }
        String value = settings.remove(primitive.valueSetting());
        String waveform = settings.remove(DefinitionWords.WAVEFORM);
        if ((waveform != null && (waveform.equals("sine") || waveform.equals("square"))) || frequency != null) {
            // A periodic source is a component whose voltage its own document writes as an equation of the time.
            throw new IllegalArgumentException(what + ": part '" + part.id() + "' alternates, so it is a generator: '"
                    + part.id() + " " + (waveform != null && waveform.equals("square") ? "square-generator (positive = …, negative = …) "
                    + "high = …, frequency = …, duty = …" : "sine-generator (positive = …, negative = …) amplitude = …, frequency = …")
                    + "'. A voltage-source is constant.");
        }
        if (waveform != null) {
            // 'step' and 'dc' were one thing: every circuit starts from rest, so every source is switched on at the start.
            throw new IllegalArgumentException(what + ": part '" + part.id() + "' writes 'waveform = " + waveform + "', which is gone: "
                    + "a circuit starts from rest, so a source is switched on at the start — leave the word out.");
        }
        // What is left are its component's other parameters — a load's power_rating, a part's tolerance —, checked
        // against that component when the circuit is compiled, where the catalogue is at hand.
        return new CircuitElement(part.id(), primitive.type(), part.positional().get(0), part.positional().get(1), value,
                null, null, settings, part.symbol().orElse(null));
    }

    /**
     * A part's value as every reader of the netlist evaluates it: one written as a datasheet does — {@code 100 kΩ},
     * {@code 4.7 nF}, {@code 12 V} — in SI, as in a component's {@code parameters}; an expression over the parameters as
     * written. The text of the document keeps the value as it was written.
     */
    private static String inSi(String value) {
        if (!Quantity.isQuantity(value)) {
            return value;
        }
        return BigDecimal.valueOf(Quantity.valueOf(value)).stripTrailingZeros().toPlainString();
    }

    /** {@code analysis transient for 0.01 s}, or {@code from 1 ms to 10 ms} for a span that does not start at 0. */
    private Interval transientSpan() {
        Block.Analysis analysis = document.blocks(Block.Analysis.class).stream()
                .filter(written -> written.kind() == Block.AnalysisKind.TRANSIENT).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(what + " writes 'analysis transient for <time> s'."));
        Map<String, SourceText> settings = analysis.settings();
        if (settings.containsKey("for")) {
            return new Interval(0, Notation.parseQuantity(settings.get("for").text(), "the transient span of " + what, "s"));
        }
        if (settings.containsKey("from") && settings.containsKey("to")) {
            return new Interval(Notation.parseQuantity(settings.get("from").text(), "the transient start of " + what, "s"),
                    Notation.parseQuantity(settings.get("to").text(), "the transient end of " + what, "s"));
        }
        throw new IllegalArgumentException(what + " writes 'analysis transient for <time> s' or 'from <start> s to <end> s'.");
    }

    /** The drawing lines of a {@code symbol} document as the drawing JSON a symbol definition carries. */
    private ObjectDefinition symbol() {
        Block.Symbol drawn = document.block(Block.Symbol.class)
                .orElseThrow(() -> new IllegalArgumentException(what + " draws something: size, path, pin and label lines."));
        List<SymbolDrawing.Pin> pins = drawn.pins().stream()
                .map(pin -> new SymbolDrawing.Pin(pin.pin(), pin.x(), pin.y(), pin.label())).toList();
        List<SymbolDrawing.Label> labels = drawn.labels().stream()
                .map(label -> new SymbolDrawing.Label(label.subject(), label.x(), label.y())).toList();
        List<SymbolDrawing.Indicator> indicators = drawn.indicators().stream().map(indicator -> new SymbolDrawing.Indicator(
                indicator.name(), indicator.left(), indicator.top(), indicator.right(), indicator.bottom())).toList();
        return new ObjectDefinition.Symbol(new SymbolDrawing(drawn.width(), drawn.height(), drawn.paths(), pins, labels,
                indicators, drawn.upright(), LivingDrawing.inputs(drawn.inputs()),
                LivingDrawing.elements(what, drawn.inputs(), drawn.elements())).toJson());
    }

    // ─── graphs ─────────────────────────────────────────────────────────────────────────────────────

    /** The graph as written: its own definitions, its clock, its panes (one plane when none) and its lines. */
    private ObjectDefinition graph() {
        String formulas = document.block(Block.Formulas.class).flatMap(Block.Formulas::definitions)
                .map(SourceText::text).orElse("");
        ObjectDefinition.GraphClock clock = document.block(Block.Clock.class)
                .map(written -> new ObjectDefinition.GraphClock(written.parameter(),
                        written.range().map(range -> range.minimum().text()).orElse(null),
                        written.range().map(range -> range.maximum().text()).orElse(null), written.speed(), written.loop()))
                .orElse(null);
        Block.Panes panes = document.block(Block.Panes.class).orElse(new Block.Panes(Block.PaneLayout.ROWS,
                List.of(new Block.Pane("main", Block.PaneKind.PLANE, Map.of(), Optional.empty(), Optional.empty(),
                        Optional.empty(), 0))));
        List<ObjectDefinition.GraphPane> shownPanes = panes.panes().stream()
                .map(pane -> new ObjectDefinition.GraphPane(pane.name(), pane.kind().name(), axesOf(pane),
                        pane.uses().orElse(null),
                        pane.over().map(range -> List.of(range.minimum().text(), range.maximum().text())).orElse(null),
                        pane.camera().orElse(null)))
                .toList();
        List<ObjectDefinition.GraphLine> lines = document.block(Block.Lines.class).map(Block.Lines::lines).orElse(List.of())
                .stream()
                .map(line -> new ObjectDefinition.GraphLine(line.name(), line.expression().text(), line.label(),
                        line.until().map(SourceText::text).orElse(null), line.at().map(SourceText::text).orElse(null),
                        line.from().orElse(null), line.pane().orElse(null), line.color().orElse(null), line.hidden()))
                .toList();
        List<ObjectDefinition.GraphCheck> checks = document.blocks(Block.GraphCheck.class).stream().map(check -> {
            if (check.criterion().kind() == Block.CriterionKind.IS) {
                throw new IllegalArgumentException(what + ": '" + check.title() + "' is checked against an interval: near … or within …");
            }
            double[] bounds = check.criterion().bounds(this::amountValue);
            return new ObjectDefinition.GraphCheck(check.title(), check.measure().text(), check.written(), bounds[0], bounds[1]);
        }).toList();
        return new ObjectDefinition.Graph(formulas, clock, panes.layout().name(), shownPanes, lines, checks);
    }

    private static Map<String, List<String>> axesOf(Block.Pane pane) {
        Map<String, List<String>> axes = new LinkedHashMap<>();
        pane.axes().forEach((axis, range) -> axes.put(axis, List.of(range.minimum().text(), range.maximum().text())));
        return axes;
    }

    // ─── inputs and outputs ─────────────────────────────────────────────────────────────────────────

    private List<Input> inputList() {
        return document.block(Block.Inputs.class).map(Block.Inputs::inputs).orElse(List.of());
    }

    /** The names of the inputs, each its own variable, as many as the structure needs: {@code u, v}, {@code a1, a2}. */
    private List<String> inputNames(int count) {
        List<String> names = inputList().stream().flatMap(input -> input.names().stream()).toList();
        if (names.size() != count) {
            throw new IllegalArgumentException(what + " has " + count + " input(s) here, not " + names.size() + ": "
                    + "one line each, 'name in a .. b'.");
        }
        return names;
    }

    /** The one input of a curve or a plane iteration. */
    private Input onlyInput() {
        List<Input> inputs = inputList();
        if (inputs.size() != 1 || inputs.getFirst().names().size() != 1) {
            throw new IllegalArgumentException(what + " has one input: 'x in a .. b', 't in 0 .. 2 pi', "
                    + "'n in 1 .. 100 integer' or '(z) in -2 - 2 i .. 2 + 2 i'.");
        }
        return inputs.getFirst();
    }

    private Input input(String name) {
        return inputList().stream().filter(input -> input.names().contains(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(what + " has an input '" + name + " in a .. b'."));
    }

    private Block.Range range(Input input, String name) {
        return input.range().orElseThrow(() -> new IllegalArgumentException(
                what + " writes the range of '" + name + "': '" + name + " in a .. b'."));
    }

    private Interval interval(String name) {
        return interval(range(input(name), name), name);
    }

    /** One interval per axis, x first: a vector field's box, a non-linear map's domain. */
    private List<Interval> axisIntervals(int dimension) {
        return AXES.subList(0, dimension).stream().map(this::interval).toList();
    }

    private Interval interval(Block.Range range, String name) {
        String of = "the range of '" + name + "' in " + what;
        return new Interval(Notation.parseNumber(range.minimum().text(), of), Notation.parseNumber(range.maximum().text(), of));
    }

    private Output output() {
        List<Output> outputs = document.outputs();
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException(what + " has an 'outputs' block saying what it computes.");
        }
        return outputs.getFirst();
    }

    /** The expression of the first output, which is one expression. */
    private String singleOutput() {
        Output output = output();
        if (output.components().isEmpty()) {
            throw new IllegalArgumentException(what + ": output '" + output.name() + "' is written '= expression'.");
        }
        return joined(output.components());
    }

    /** The coordinates of the first output, a tuple. */
    private List<String> tupleOutput() {
        Output output = output();
        if (output.components().size() < 2) {
            throw new IllegalArgumentException(what + ": output '" + output.name() + "' is a tuple '(…, …)'.");
        }
        return texts(output.components());
    }

    /** The expression of an implicit output's {@code where}. */
    private String implicitExpression() {
        return output().equation().map(SourceText::text).orElseThrow(() -> new IllegalArgumentException(
                what + " is written 'boundary : volume where expression'."));
    }

    /** A one-coordinate output's expression; a value that is a tuple is given back whole. */
    private static String joined(List<SourceText> components) {
        return components.size() == 1 ? components.getFirst().text()
                : "(" + String.join(", ", texts(components)) + ")";
    }

    private static List<String> texts(List<SourceText> texts) {
        return texts.stream().map(SourceText::text).toList();
    }

    private static List<List<Double>> numberRows(String written, String what) {
        return Notation.rows(written, what).stream()
                .map(row -> row.stream().map(entry -> Notation.parseNumber(entry, what)).toList()).toList();
    }
}
