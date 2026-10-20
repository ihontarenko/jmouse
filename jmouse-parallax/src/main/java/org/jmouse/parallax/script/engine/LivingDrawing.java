package org.jmouse.parallax.script.engine;

import org.jmouse.formula.Formula;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.Block.SymbolElement;
import org.jmouse.parallax.script.Block.SymbolInput;
import org.jmouse.parallax.script.Block.SymbolMotion;
import org.jmouse.parallax.script.Block.SymbolProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A drawing's living part — its {@code input}s and elements — as the {@link SymbolDrawing} the browser draws from.
 * Every value is read as jMF here and handed on as an expression tree, so the browser only evaluates and never parses;
 * and every value is checked to be marked as it moves: {@code live} when it reads an input, {@code animate} when it
 * moves in time by itself, nothing when it is constant — and every block holding something that changes is
 * {@code live} too. A mark that does not say what the value does is refused, so the marks can be trusted at a glance.
 *
 * <p>An expression tree is a JSON value: a number; a name — an input, or a word such as {@code burned} the sheet
 * colours by its tokens; or an array {@code [operator, operand…]}: {@code ["+", a, b]}, {@code ["-", a]},
 * {@code ["<", a, b]}, {@code ["and", a, b]}, {@code ["not", a]}, {@code ["if", condition, value, …, otherwise]} and
 * {@code ["call", function, argument…]}.</p>
 */
final class LivingDrawing {

    /** The drawing functions the browser evaluates; {@link #TIMED} among them move on the wall clock. */
    static final Set<String> FUNCTIONS = Set.of("between", "share", "seen", "curve", "smooth", "blink", "pulse", "wave",
            "bit", "absolute", "minimum", "maximum");

    /** The functions that move in time by themselves. */
    static final Set<String> TIMED = Set.of("blink", "pulse", "wave");

    /** The jMF functions a drawing may call besides its own. */
    private static final Set<String> MATHEMATICS = Set.of("sin", "cos", "tan", "sqrt", "exp", "ln", "abs", "sign", "floor",
            "ceil", "round", "step", "min", "max");

    /** The words a value may be: what the sheet colours by its own tokens. */
    static final Set<String> WORDS = Set.of("clear", "signal", "positive", "negative", "warm", "hot", "burned", "glow",
            "red", "orange", "yellow", "green", "blue", "violet", "white", "yes", "no");

    /** Each property and how many values it is written with. */
    private static final Map<String, Set<Integer>> PROPERTIES = Map.ofEntries(
            Map.entry("rotate", Set.of(1)), Map.entry("move", Set.of(2)), Map.entry("scale", Set.of(1, 2)),
            Map.entry("opacity", Set.of(1)), Map.entry("visible", Set.of(1)), Map.entry("fill", Set.of(1)),
            Map.entry("stroke", Set.of(1)), Map.entry("glow", Set.of(1)), Map.entry("level", Set.of(1)),
            Map.entry("dash", Set.of(1)), Map.entry("thickness", Set.of(1)), Map.entry("character", Set.of(1)));

    private static final FormulaLanguage LANGUAGE = FormulaLanguage.standard().withHostCalls(FUNCTIONS.toArray(String[]::new));

    private final String what;
    private final Set<String> inputs = new HashSet<>();
    private final Set<String> names = new HashSet<>();

    private LivingDrawing(String what) {
        this.what = what;
    }

    /** A drawing's inputs as the browser reads them. */
    static List<SymbolDrawing.Input> inputs(List<SymbolInput> inputs) {
        return inputs.stream().map(input -> new SymbolDrawing.Input(input.name(), input.low(), input.high(),
                input.range().map(range -> range.text().strip()).orElse(""))).toList();
    }

    /**
     * A drawing's elements, every value read and checked.
     *
     * @param what how the document is named in a refusal
     * @throws ExperienceParseException at the line, when a value does not read, names what is not there, or is
     *                                  marked otherwise than it moves
     */
    static List<SymbolDrawing.Element> elements(String what, List<SymbolInput> inputs, List<SymbolElement> elements) {
        LivingDrawing drawing = new LivingDrawing(what);
        for (SymbolInput input : inputs) {
            if (WORDS.contains(input.name())) {
                throw new IllegalArgumentException(what + ": input '" + input.name() + "' is named as a word a drawing is "
                        + "drawn in; name it otherwise ('failed' rather than 'burned')");
            }
            drawing.inputs.add(input.name());
        }
        drawing.collectNames(elements);
        return elements.stream().map(drawing::element).toList();
    }

    private void collectNames(List<SymbolElement> elements) {
        for (SymbolElement element : elements) {
            if (!element.name().isEmpty() && !names.add(element.name())) {
                throw refusal(element, "'" + element.name() + "' names two elements");
            }
            collectNames(element.children());
        }
    }

    private SymbolDrawing.Element element(SymbolElement element) {
        SymbolDrawing.Value condition = element.condition() == null ? null : condition(element);
        Map<String, SymbolDrawing.Value> properties = new LinkedHashMap<>();
        element.properties().forEach(property -> properties.put(property.name(), property(element, property)));
        for (String hidden : element.hidden()) {
            if (!names.contains(hidden)) {
                throw refusal(element, "'hide " + hidden + "' names no element of this drawing");
            }
        }
        List<SymbolDrawing.Element> children = element.children().stream().map(this::element).toList();
        boolean changes = condition != null
                || properties.values().stream().anyMatch(value -> value.motion() != SymbolMotion.STILL)
                || children.stream().anyMatch(SymbolDrawing.Element::live);
        if (changes && !element.live()) {
            throw refusal(element, "something in this " + element.shape() + " changes: write 'live " + element.shape() + " …'");
        }
        if (!changes && element.live()) {
            throw refusal(element, "nothing in this " + element.shape() + " changes: 'live' says it does");
        }
        return new SymbolDrawing.Element(element.shape(), element.name(), element.live(), element.path(),
                element.geometry(), condition, properties, element.hidden(), children);
    }

    /** A layer's condition: one value that reads an input — a condition on nothing would always or never hold. */
    private SymbolDrawing.Value condition(SymbolElement element) {
        Read condition = read(element, element.condition());
        if (condition.value().motion() == SymbolMotion.STILL) {
            throw refusal(element, "'live when " + condition.value().written() + "' reads no input: it would always or never hold");
        }
        return condition.value();
    }

    /** A property's values, checked to be as many as it takes and marked as they move. */
    private SymbolDrawing.Value property(SymbolElement element, SymbolProperty property) {
        Set<Integer> counts = PROPERTIES.get(property.name());
        if (counts == null) {
            throw refusal(element, "'" + property.name() + "' is not a property: "
                    + String.join(", ", new TreeSet<>(PROPERTIES.keySet())));
        }
        if (!counts.contains(property.formulas().size())) {
            throw refusal(element, "'" + property.name() + "' takes " + String.join(" or ",
                    new TreeSet<>(counts).stream().map(String::valueOf).toList()) + " value(s), not " + property.formulas().size());
        }
        Read read = read(element, property);
        SymbolMotion moves = read.value().motion();
        if (moves != property.motion()) {
            String line = property.name() + " = " + read.value().written();
            throw refusal(element, switch (moves) {
                case ANIMATED -> "'" + property.name() + "' moves in time by itself: write 'animate " + line + "'";
                case LIVE -> "'" + property.name() + "' follows " + String.join(", ", read.inputs()) + ": write 'live " + line + "'";
                case STILL -> "'" + line + "' never changes: it is written without '" + property.motion().name().toLowerCase(Locale.ROOT) + "'";
            });
        }
        return read.value();
    }

    /** A value read: its trees and how they move, and the inputs it follows, for a refusal to name. */
    private record Read(SymbolDrawing.Value value, Set<String> inputs) {
    }

    /** A value's formulas read as jMF and turned into trees; it moves as what it reads says. */
    private Read read(SymbolElement element, SymbolProperty property) {
        List<Object> trees = new ArrayList<>();
        Reads reads = new Reads();
        String written = property.written().text().strip();
        for (String formula : property.formulas()) {
            Formula parsed;
            try {
                parsed = LANGUAGE.parse(formula);
            } catch (RuntimeException unreadable) {
                throw refusal(element, "'" + property.name() + " = " + written + "' does not read: " + unreadable.getMessage());
            }
            trees.add(tree(element, parsed, reads));
        }
        SymbolMotion motion = reads.timed ? SymbolMotion.ANIMATED : reads.follows ? SymbolMotion.LIVE : SymbolMotion.STILL;
        return new Read(new SymbolDrawing.Value(written, trees, motion), reads.inputs);
    }

    /** What a value reads, gathered while its trees are made: the inputs it follows, and whether it moves by itself. */
    private static final class Reads {
        private final Set<String> inputs = new TreeSet<>();
        private boolean follows;
        private boolean timed;
    }

    private Object tree(SymbolElement element, Formula formula, Reads reads) {
        return switch (formula) {
            case Formula.Constant constant -> constant.value();
            case Formula.NamedConstant named -> named.constant().value();
            case Formula.Variable variable -> {
                if (inputs.contains(variable.name())) {
                    reads.follows = true;
                    reads.inputs.add(variable.name());
                } else if (!WORDS.contains(variable.name())) {
                    throw refusal(element, "'" + variable.name() + "' is neither an input of this drawing ("
                            + (inputs.isEmpty() ? "it declares none" : String.join(", ", new TreeSet<>(inputs)))
                            + ") nor a word it may be drawn in (" + String.join(", ", new TreeSet<>(WORDS)) + ")");
                }
                yield variable.name();
            }
            case Formula.Negation negation -> List.of("-", tree(element, negation.operand(), reads));
            case Formula.Binary binary -> List.of(binary.operator().symbol(), tree(element, binary.left(), reads),
                    tree(element, binary.right(), reads));
            case Formula.Comparison comparison -> List.of(comparison.relation().symbol(), tree(element, comparison.left(), reads),
                    tree(element, comparison.right(), reads));
            case Formula.Logical logical -> List.of(logical.connective().symbol(), tree(element, logical.left(), reads),
                    tree(element, logical.right(), reads));
            case Formula.Not not -> List.of("not", tree(element, not.operand(), reads));
            case Formula.Piecewise piecewise -> {
                List<Object> branches = new ArrayList<>(List.of("if"));
                for (Formula.Piecewise.Branch branch : piecewise.branches()) {
                    branches.add(tree(element, branch.condition(), reads));
                    branches.add(tree(element, branch.value(), reads));
                }
                branches.add(piecewise.otherwise() == null ? "clear" : tree(element, piecewise.otherwise(), reads));
                yield branches;
            }
            case Formula.Call call -> {
                String name = call.function().symbol();
                if (!MATHEMATICS.contains(name)) {
                    throw refusal(element, "'" + name + "' is not drawn: a drawing calls " + String.join(", ",
                            new TreeSet<>(FUNCTIONS)) + " and " + String.join(", ", new TreeSet<>(MATHEMATICS)));
                }
                yield call(element, name, call.arguments(), reads);
            }
            case Formula.HostCall call -> {
                if (TIMED.contains(call.name())) {
                    reads.timed = true;
                }
                yield call(element, call.name(), call.arguments(), reads);
            }
            default -> throw refusal(element, "a drawing's value is arithmetic, comparisons, 'if' and calls over its "
                    + "inputs and words");
        };
    }

    private List<Object> call(SymbolElement element, String name, List<Formula> arguments, Reads reads) {
        List<Object> call = new ArrayList<>(List.of("call", name));
        arguments.forEach(argument -> call.add(tree(element, argument, reads)));
        return call;
    }

    /** A refusal at the element's line, as the reader's own refusals are: an editor marks the line it names. */
    private ExperienceParseException refusal(SymbolElement element, String detail) {
        return new ExperienceParseException(what, element.line(), 0, detail + ".");
    }
}
