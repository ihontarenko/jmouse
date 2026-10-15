package org.jmouse.parallax.store.formula;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.dimension.PolytopeSkeleton;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.Randomness;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One formula in the three notations the interface switches between: LaTeX to typeset, jMF as it is typed, and
 * plain text to read or paste. A typed formula is rendered from its source; an object is rendered from its
 * definition, each expression under the name it defines ({@code z = …}, {@code x(t) = …}).
 */
public class FormulaNotations {

    /** The three notations; any may be null when the formula cannot be written that way. */
    public record Notation(String latex, String jmf, String text) {
    }

    /** A typed formula rendered, or refused with words and the position the parser stopped at. */
    public record Rendering(String source, Notation notation, String refusal, Integer position) {
    }

    private record Label(String latex, String jmf, String text) {

        /** No name: a condition is printed as it is, not as what it defines. */
        static final Label NONE = new Label("", "", "");

        static Label of(String name) {
            return new Label(name, name, name);
        }
    }

    private record Line(Label label, String expression) {
    }

    private static final int MAXIMUM_SOURCE = 4000;
    private static final int MAXIMUM_SOURCES = 500;
    private static final List<String> AXES = List.of("x", "y", "z", "w");

    /**
     * jMF with the functions engines supply — {@code random_sign()}, {@code white_noise()} — read as calls, so a random
     * walk's step prints as written instead of falling back to plain text.
     */
    private static final FormulaLanguage LANGUAGE = FormulaLanguage.standard().withHostCalls(java.util.stream.Stream.concat(
            java.util.Arrays.stream(Randomness.Draw.values()).map(Randomness.Draw::functionName),
            java.util.stream.Stream.of(PolytopeSkeleton.HAMMING, PolytopeSkeleton.DOT, PolytopeSkeleton.DISTANCE))
            .toArray(String[]::new));

    private static final Map<String, String> GREEK = Map.ofEntries(
            Map.entry("alpha", "α"), Map.entry("beta", "β"), Map.entry("gamma", "γ"), Map.entry("delta", "δ"),
            Map.entry("epsilon", "ε"), Map.entry("theta", "θ"), Map.entry("lambda", "λ"), Map.entry("mu", "μ"),
            Map.entry("rho", "ρ"), Map.entry("sigma", "σ"), Map.entry("tau", "τ"), Map.entry("phi", "φ"),
            Map.entry("omega", "ω"), Map.entry("pi", "π"));
    private static final Pattern WORD = Pattern.compile("\\b([A-Za-z]+)\\b");
    private static final Pattern SMALL_POWER = Pattern.compile("\\^([0-9])(?![0-9.])");
    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    private static final Map<String, String> SUBSCRIPTS = Map.of("i", "ᵢ", "j", "ⱼ", "k", "ₖ", "m", "ₘ", "n", "ₙ", "t", "ₜ");

    /** Renders typed jMF sources in one call, in the order given. */
    public List<Rendering> render(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            throw ParallaxProblem.invalid("Give at least one formula.");
        }
        if (sources.size() > MAXIMUM_SOURCES) {
            throw ParallaxProblem.invalid("At most " + MAXIMUM_SOURCES + " formulas are rendered in one call.");
        }
        return sources.stream().map(this::render).toList();
    }

    public Rendering render(String source) {
        if (source == null || source.isBlank()) {
            return new Rendering(source, null, "Type a formula.", null);
        }
        if (source.length() > MAXIMUM_SOURCE) {
            return new Rendering(source, null, "A formula is at most " + MAXIMUM_SOURCE + " characters.", null);
        }
        try {
            Formula formula = FormulaLanguage.standard().parse(source);
            String text = FormulaPrinter.text(formula);
            return new Rendering(source, new Notation(FormulaPrinter.latex(formula), FormulaPrinter.document(formula), readable(text)), null, null);
        } catch (FormulaException refused) {
            Integer position = refused.position() < 0 ? Integer.valueOf(source.length()) : Integer.valueOf(refused.position());
            return new Rendering(source, null, refused.getMessage(), position);
        }
    }

    /**
     * The notation of an object: its definition's expressions, each under the name it defines. A hand-written
     * LaTeX, when the object has one, is preferred for typesetting — it is the curated form.
     */
    public Notation describe(ObjectDefinition definition, String curatedLatex) {
        List<Line> lines = definition == null ? List.of() : linesOf(definition);
        String latex = curatedLatex == null || curatedLatex.isBlank() ? null : curatedLatex;
        if (lines.isEmpty()) {
            return latex == null ? null : new Notation(latex, null, null);
        }
        List<String> latexLines = new ArrayList<>();
        List<String> jmfLines = new ArrayList<>();
        List<String> textLines = new ArrayList<>();
        for (Line line : lines) {
            // Local definitions in front of an expression — c = c_real + c_imaginary i; z^2 + c — are lines of their
            // own, each read against the ones before it, so none is printed inlined into the next.
            List<String> statements = statements(line.expression());
            if (statements.isEmpty()) {
                // A part with no value of its own — a switch, a model — has nothing to print.
                continue;
            }
            List<Formula.Definition> library = new ArrayList<>();
            for (String statement : statements.subList(0, statements.size() - 1)) {
                int equals = statement.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                Line local = new Line(Label.of(statement.substring(0, equals).strip()),
                        statement.substring(equals + 1).strip());
                write(local, library, latexLines, jmfLines, textLines);
                try {
                    library.addAll(LANGUAGE.definitions(statement, library));
                } catch (FormulaException unreadable) {
                    // The line is already written as text; what follows is read without it.
                }
            }
            write(new Line(line.label(), statements.getLast()), library, latexLines, jmfLines, textLines);
        }
        if (latex == null) {
            latex = latexLines.size() == 1 ? latexLines.getFirst()
                    : "\\begin{aligned}" + String.join(" \\\\ ", latexLines.stream().map(line -> line.replaceFirst(" = ", " &= ")).toList()) + "\\end{aligned}";
        }
        return new Notation(latex, String.join("\n", jmfLines), String.join("\n", textLines));
    }

    private static void write(Line line, List<Formula.Definition> library, List<String> latexLines,
                              List<String> jmfLines, List<String> textLines) {
        boolean named = !line.label().jmf().isEmpty();
        jmfLines.add(named ? line.label().jmf() + " = " + line.expression() : line.expression());
        try {
            Formula formula = LANGUAGE.parse(line.expression(), library);
            latexLines.add((named ? line.label().latex() + " = " : "") + FormulaPrinter.latex(formula));
            textLines.add((named ? line.label().text() + " = " : "") + readable(FormulaPrinter.text(formula)));
        } catch (FormulaException unreadable) {
            latexLines.add((named ? line.label().latex() + " = " : "") + "\\text{" + line.expression().replace("\\", "") + "}");
            textLines.add((named ? line.label().text() + " = " : "") + line.expression());
        }
    }

    /** An expression's statements, split at the {@code ;} outside brackets: its local definitions, then itself. */
    private static List<String> statements(String expression) {
        List<String> statements = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < expression.length(); index++) {
            char character = expression.charAt(index);
            if (character == '(' || character == '[') {
                depth++;
            } else if (character == ')' || character == ']') {
                depth--;
            } else if (character == ';' && depth == 0) {
                statements.add(expression.substring(start, index).strip());
                start = index + 1;
            }
        }
        statements.add(expression.substring(start).strip());
        return statements.stream().filter(statement -> !statement.isEmpty()).toList();
    }

    private static List<Line> linesOf(ObjectDefinition definition) {
        return switch (definition) {
            case ObjectDefinition.ExplicitCurve curve -> List.of(new Line(Label.of("y"), curve.expression()));
            case ObjectDefinition.ExplicitSurface surface -> List.of(new Line(Label.of("z"), surface.expression()));
            case ObjectDefinition.ImplicitCurve curve -> List.of(new Line(Label.of("0"), curve.expression()));
            case ObjectDefinition.ImplicitSurface surface -> List.of(new Line(Label.of("0"), surface.expression()));
            case ObjectDefinition.ImplicitHypersurface surface -> List.of(new Line(Label.of("0"), surface.expression()));
            case ObjectDefinition.ParametricCurve curve -> components(curve.components(), curve.variables().getFirst());
            case ObjectDefinition.ParametricSurface surface -> components(surface.components(),
                    String.join(", ", surface.variables()));
            case ObjectDefinition.NonlinearTransformation transformation -> components(transformation.components(), null);
            case ObjectDefinition.VectorField field -> indexed(field.components(), "F");
            case ObjectDefinition.DifferentialSystem system -> derivatives(system.variables(), system.equations());
            case ObjectDefinition.DiscreteMap map -> {
                List<Line> lines = new ArrayList<>(map.stepValues().stream()
                        .map(value -> new Line(Label.of(value.name()), value.expression())).toList());
                lines.addAll(successors(map.variables(), map.updates()));
                yield lines;
            }
            case ObjectDefinition.ComplexMapping mapping ->
                    List.of(new Line(Label.of("f(" + mapping.variables().getFirst() + ")"), mapping.expression()));
            case ObjectDefinition.EscapeTimeFractal fractal -> List.of(new Line(successor(fractal.state()), fractal.iteration()));
            case ObjectDefinition.NewtonFractal fractal -> List.of(new Line(successor(fractal.state()), fractal.iteration()));
            case ObjectDefinition.ProbabilityDistribution distribution ->
                    List.of(new Line(Label.of("f(" + String.join(", ", distribution.variables()) + ")"), distribution.density()));
            case ObjectDefinition.Region region -> List.of(new Line(Label.NONE, region.condition()));
            case ObjectDefinition.Sequence sequence -> List.of(new Line(term(sequence.variables().getFirst()), sequence.expression()));
            case ObjectDefinition.Value value -> List.of(new Line(named(value.name()), value.expression()));
            case ObjectDefinition.IteratedFunctionSystem ignored -> List.of();
            case ObjectDefinition.LindenmayerSystem ignored -> List.of();
            case ObjectDefinition.MatrixObject ignored -> List.of();
            case ObjectDefinition.AffineTransformation ignored -> List.of();
            case ObjectDefinition.PointSet ignored -> List.of();
            case ObjectDefinition.GeometricConstruction ignored -> List.of();
            case ObjectDefinition.Polytope ignored -> List.of();
            case ObjectDefinition.Program program -> definitionLines(program.formulas());
            case ObjectDefinition.Component ignored -> List.of();
            case ObjectDefinition.Symbol ignored -> List.of();
            case ObjectDefinition.Graph ignored -> List.of();
            case ObjectDefinition.Family family -> linesOf(family.member());
            case ObjectDefinition.Circuit circuit -> circuit.elements().stream()
                    .map(element -> new Line(Label.of(element.id()), element.value()))
                    .toList();
        };
    }

    /** A value's output: a word set upright in LaTeX ({@code \mathrm{root}}), a letter in italics. */
    private static Label named(String name) {
        return new Label(name.length() > 1 ? "\\mathrm{" + name + "}" : name, name, name);
    }

    /** {@code z_{n+1}}: the state a step defines. */
    private static Label successor(String state) {
        return new Label(state + "_{n+1}", state + "_next", state + "ₙ₊₁");
    }

    /** {@code a_k}: the term of a sequence over the index its author named. */
    private static Label term(String index) {
        String subscript = SUBSCRIPTS.get(index);
        return new Label("a_{" + index + "}", "a(" + index + ")", subscript == null ? "a(" + index + ")" : "a" + subscript);
    }

    /** A program's formulas, one line per definition: {@code f(x) = …} as written. */
    private static List<Line> definitionLines(String formulas) {
        List<Line> lines = new ArrayList<>();
        for (String line : formulas.split("\\R")) {
            String written = line.strip();
            int equals = written.indexOf('=');
            if (written.isEmpty() || written.startsWith("#") || equals <= 0) {
                continue;
            }
            lines.add(new Line(Label.of(written.substring(0, equals).strip()), written.substring(equals + 1).strip()));
        }
        return lines;
    }

    /** x(t) = …, y(t) = …; a transformation's components are the primed axes. */
    private static List<Line> components(List<String> expressions, String arguments) {
        List<Line> lines = new ArrayList<>();
        for (int index = 0; index < expressions.size(); index++) {
            String axis = index < AXES.size() ? AXES.get(index) : "x" + (index + 1);
            Label label = arguments == null ? new Label(axis + "'", axis + "_image", axis + "′")
                    : Label.of(axis + "(" + arguments + ")");
            lines.add(new Line(label, expressions.get(index)));
        }
        return lines;
    }

    private static List<Line> indexed(List<String> expressions, String name) {
        List<Line> lines = new ArrayList<>();
        for (int index = 0; index < expressions.size(); index++) {
            String axis = index < AXES.size() ? AXES.get(index) : Integer.toString(index + 1);
            lines.add(new Line(new Label(name + "_{" + axis + "}", name + "_" + axis, name + axis), expressions.get(index)));
        }
        return lines;
    }

    private static List<Line> derivatives(List<String> variables, List<String> equations) {
        List<Line> lines = new ArrayList<>();
        for (int index = 0; index < equations.size(); index++) {
            String variable = index < variables.size() ? variables.get(index) : "x" + (index + 1);
            lines.add(new Line(new Label("\\dot{" + variable + "}", "d" + variable + "/dt", variable + "̇"), equations.get(index)));
        }
        return lines;
    }

    private static List<Line> successors(List<String> variables, List<String> updates) {
        List<Line> lines = new ArrayList<>();
        for (int index = 0; index < updates.size(); index++) {
            String variable = index < variables.size() ? variables.get(index) : "x" + (index + 1);
            lines.add(new Line(new Label(variable + "_{n+1}", variable + "_next", variable + "ₙ₊₁"), updates.get(index)));
        }
        return lines;
    }

    /** The plain notation made pleasant to read: ·, √, ≤, Greek letters and small powers as superscripts. */
    static String readable(String text) {
        String readable = text.replace(" * ", " · ").replace("*", "·").replace("<=", "≤").replace(">=", "≥")
                .replace("!=", "≠").replace("sqrt(", "√(");
        Matcher words = WORD.matcher(readable);
        StringBuilder greek = new StringBuilder();
        while (words.find()) {
            words.appendReplacement(greek, Matcher.quoteReplacement(GREEK.getOrDefault(words.group(1), words.group(1))));
        }
        words.appendTail(greek);
        Matcher powers = SMALL_POWER.matcher(greek.toString());
        StringBuilder superscripted = new StringBuilder();
        while (powers.find()) {
            powers.appendReplacement(superscripted, String.valueOf(SUPERSCRIPTS.charAt(powers.group(1).charAt(0) - '0')));
        }
        powers.appendTail(superscripted);
        return superscripted.toString();
    }
}
