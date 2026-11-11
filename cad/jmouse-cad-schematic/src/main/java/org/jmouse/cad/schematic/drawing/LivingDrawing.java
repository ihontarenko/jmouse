package org.jmouse.cad.schematic.drawing;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.cad.schematic.DrawingGrammar;
import org.jmouse.cad.schematic.drawing.SymbolDrawing.Element;
import org.jmouse.cad.schematic.drawing.SymbolDrawing.Motion;
import org.jmouse.cad.schematic.drawing.SymbolDrawing.Value;
import org.jmouse.el.grammar.AssignedNode;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ExtentNode;
import org.jmouse.el.grammar.FormulaNode;
import org.jmouse.el.grammar.ListNode;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.grammar.RangeNode;
import org.jmouse.el.grammar.WordNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.formula.Formula;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A drawing document — a symbol, a portrait, a frame — as the interface draws it ({@link SymbolDrawing}): its lines
 * read off its tree, its living part checked. Every value is read as jMF and handed on as a tree, and every value is
 * checked to be marked as it moves — {@code ->} pairs two values of a {@code curve}, read by jMF in the tree —: {@code live} when it reads an input, {@code animate} when it moves in time by itself,
 * nothing when it is constant — and every block holding something that changes is {@code live} too. A mark that does
 * not say what the value does is refused, so the marks can be trusted at a glance.
 */
public final class LivingDrawing {

    /** What draws a still path and an element by the words its document writes. */
    private static final String PATH = "path";
    private static final String LAYER = "when";
    private static final String UPRIGHT = "upright";
    private static final String HIDE = "hide";

    /** The words an element is placed by, in the order the interface reads them. */
    private static final List<String> GEOMETRY = List.of("at", "size", "radius", "from", "to", "pivot");

    private static final FormulaLanguage LANGUAGE = FormulaLanguage.standard().withQuantities().withPairs()
            .withHostCalls(DrawingVocabulary.FUNCTIONS.toArray(String[]::new));

    private final String       what;
    private final Set<String>  inputs   = new HashSet<>();
    /** The inputs that are pictures: read by a bitmap's {@code pixels} alone, never as a number. */
    private final Set<String>  pictures = new HashSet<>();
    /** The inputs that are texts: printed by a glyph's {@code text} alone, never read as a number. */
    private final Set<String>  texts    = new HashSet<>();
    private final Set<String>  names    = new HashSet<>();

    private LivingDrawing(String what) {
        this.what = what;
    }

    /**
     * A drawing document as the interface draws it.
     *
     * @throws IllegalArgumentException when a value does not read, names what is not there, or is marked otherwise than
     *                                  it moves
     */
    public static SymbolDrawing of(DocumentNode document) {
        LivingDrawing drawing = new LivingDrawing(document.title());
        double width = 0;
        double height = 0;
        List<String> paths = new ArrayList<>();
        List<String> upright = new ArrayList<>();
        List<SymbolDrawing.Pin> pins = new ArrayList<>();
        List<SymbolDrawing.Label> labels = new ArrayList<>();
        List<SymbolDrawing.Indicator> indicators = new ArrayList<>();
        List<SymbolDrawing.Input> inputs = new ArrayList<>();
        List<ClauseBlockNode> elements = new ArrayList<>();
        for (Expression statement : document.getExpressions()) {
            switch (statement) {
                case ClauseBlockNode block when isStillPath(block) -> {
                    ClauseLineNode header = block.header().orElseThrow();
                    (header.has(UPRIGHT) ? upright : paths).add(pathOf(header));
                }
                case ClauseBlockNode block when block.header().isPresent() -> elements.add(block);
                case ClauseLineNode line when line.grammar() == DrawingGrammar.SIZE -> {
                    ExtentNode extent = (ExtentNode) line.slot("extent").orElseThrow();
                    width = extent.width();
                    height = extent.height();
                }
                case ClauseLineNode line when line.grammar() == DrawingGrammar.PIN -> {
                    PointNode at = line.point("at").orElseThrow();
                    pins.add(new SymbolDrawing.Pin(line.name("name").orElseThrow(), at.x(), at.y(), line.ending().orElse("")));
                }
                case ClauseLineNode line when line.grammar() == DrawingGrammar.LABEL -> {
                    PointNode at = line.point("at").orElseThrow();
                    labels.add(new SymbolDrawing.Label(line.name("subject").orElseThrow(), at.x(), at.y()));
                }
                case ClauseLineNode line when line.grammar() == DrawingGrammar.INDICATOR -> {
                    PointNode from = line.point("from").orElseThrow();
                    PointNode to = line.point("to").orElseThrow();
                    indicators.add(new SymbolDrawing.Indicator(line.name("name").orElseThrow(), from.x(), from.y(), to.x(), to.y()));
                }
                case ClauseLineNode line when line.grammar() == DrawingGrammar.INPUT -> inputs.add(drawing.input(line));
                default -> {
                }
            }
        }
        drawing.collectNames(elements);
        List<Element> drawn = elements.stream().map(drawing::element).toList();
        return new SymbolDrawing(width, height, paths, upright, pins, labels, indicators, inputs, drawn);
    }

    /** A {@code path "…" [upright]} with no name, no {@code live} and no body: a still path, drawn as it stands. */
    static boolean isStillPath(ClauseBlockNode block) {
        if (!block.word().equals(PATH) || block.opened() || block.header().isEmpty()) {
            return false;
        }
        ClauseLineNode header = block.header().get();
        return header.prefixes().isEmpty() && header.name("name").isEmpty() && GEOMETRY.stream().noneMatch(header::has);
    }

    private static String pathOf(ClauseLineNode header) {
        return header.endingValue().map(value -> ((PathTextNode) value).path().toSource()).orElse("");
    }

    /** {@code input current 0 .. 20 mA [typical 5 mA]}, {@code input screen picture}, {@code input marking text ["IC"]}. */
    private SymbolDrawing.Input input(ClauseLineNode line) {
        String name = line.name("name").orElseThrow();
        if (DrawingVocabulary.WORDS.contains(name)) {
            throw new IllegalArgumentException(what + ": input '" + name + "' is named as a word a drawing is drawn in; name it "
                    + "otherwise ('failed' rather than 'burned')");
        }
        if (!inputs.add(name)) {
            throw new IllegalArgumentException(what + ": input '" + name + "' is declared twice");
        }
        if (line.has("picture")) {
            pictures.add(name);
            return new SymbolDrawing.Input(name, 0, 1, 0, "", true, null);
        }
        if (line.has("text")) {
            texts.add(name);
            return new SymbolDrawing.Input(name, 0, 1, 0, "", false, line.ending().orElse(""));
        }
        if (line.slot("range").isEmpty()) {
            return new SymbolDrawing.Input(name, 0, 1, 0, "", false, null);
        }
        RangeNode range = (RangeNode) line.slot("range").get();
        double low = Quantity.valueOf(range.low().orElseThrow().written());
        double high = Quantity.valueOf(range.high().orElseThrow().written());
        double typical = line.quantity("typical").orElse(low);
        return new SymbolDrawing.Input(name, low, high, typical, TextSourceWriter.render(range, SourceLayout.WORDS), false, null);
    }

    private void collectNames(List<ClauseBlockNode> elements) {
        for (ClauseBlockNode element : elements) {
            String name = nameOf(element);
            if (!name.isEmpty() && !names.add(name)) {
                throw refusal("'" + name + "' names two elements");
            }
            collectNames(childrenOf(element));
        }
    }

    private Element element(ClauseBlockNode element) {
        ClauseLineNode header = element.header().orElseThrow();
        boolean layer = element.word().equals(LAYER);
        boolean live = header.prefixes().contains(DrawingGrammar.LIVE);
        Value condition = layer ? condition(header) : null;
        Map<String, Value> properties = new LinkedHashMap<>();
        List<String> hidden = new ArrayList<>();
        for (Expression statement : element.getExpressions()) {
            if (statement instanceof ClauseLineNode line && line.grammar() == DrawingGrammar.PROPERTY) {
                properties.put(line.name("name").orElseThrow(), property(element, line));
            } else if (statement instanceof ClauseLineNode line && line.word().equals(HIDE)) {
                for (String name : line.slotNames("names")) {
                    if (!names.contains(name)) {
                        throw refusal("'hide " + name + "' names no element of this drawing");
                    }
                    hidden.add(name);
                }
            }
        }
        List<Element> children = childrenOf(element).stream().map(this::element).toList();
        boolean changes = condition != null || properties.values().stream().anyMatch(value -> value.motion() != Motion.STILL)
                || children.stream().anyMatch(Element::live);
        if (changes && !live) {
            throw refusal("something in this " + element.word() + " changes: write 'live " + element.word() + " …'");
        }
        if (!changes && live) {
            throw refusal("nothing in this " + element.word() + " changes: 'live' says it does");
        }
        return new Element(element.word(), nameOf(element), live, layer ? "" : pathOf(header), geometry(header), condition,
                properties, hidden, children);
    }

    private static String nameOf(ClauseBlockNode element) {
        return element.header().flatMap(header -> header.name("name")).orElse("");
    }

    private static List<ClauseBlockNode> childrenOf(ClauseBlockNode element) {
        return element.getExpressions().stream().filter(ClauseBlockNode.class::isInstance).map(ClauseBlockNode.class::cast).toList();
    }

    /** An element's placing words, each its numbers: {@code at} two, {@code size} two, {@code radius} one. */
    private static Map<String, List<Double>> geometry(ClauseLineNode header) {
        Map<String, List<Double>> geometry = new LinkedHashMap<>();
        for (String word : GEOMETRY) {
            header.value(word).ifPresent(value -> geometry.put(word, switch (value) {
                case PointNode point -> List.of(point.x(), point.y());
                case ExtentNode extent -> List.of(extent.width(), extent.height());
                default -> List.of(header.quantity(word).orElseThrow());
            }));
        }
        return geometry;
    }

    /** A layer's condition: one value that reads an input — a condition on nothing would always or never hold. */
    private Value condition(ClauseLineNode header) {
        FormulaNode written = (FormulaNode) header.slot("condition").orElseThrow();
        Value condition = read(LAYER, written.written(), List.of(written.written())).value();
        if (condition.motion() == Motion.STILL) {
            throw refusal("'live when " + written.written() + "' reads no input: it would always or never hold");
        }
        return condition;
    }

    /** A property's values, checked to be as many as it takes and marked as they move. */
    private Value property(ClauseBlockNode element, ClauseLineNode line) {
        String name = line.name("name").orElseThrow();
        Motion marked = line.prefixes().contains(DrawingGrammar.ANIMATE) ? Motion.ANIMATED
                : line.prefixes().contains(DrawingGrammar.LIVE) ? Motion.LIVE : Motion.STILL;
        Expression assigned = ((AssignedNode) line.slot("value").orElseThrow()).value();
        List<String> formulas = switch (assigned) {
            case ListNode list -> list.items().stream().map(item -> ((FormulaNode) item).written()).toList();
            case FormulaNode formula -> List.of(formula.written());
            case WordNode words -> List.of(words.word());
            default -> throw refusal("'" + name + "' is given what is no value");
        };
        String written = TextSourceWriter.render(assigned, SourceLayout.WORDS).strip();
        Set<Integer> counts = DrawingVocabulary.PROPERTIES.get(name);
        if (counts == null) {
            throw refusal("'" + name + "' is not a property: " + String.join(", ", new TreeSet<>(DrawingVocabulary.PROPERTIES.keySet())));
        }
        if (!counts.contains(formulas.size())) {
            throw refusal("'" + name + "' takes " + String.join(" or ", new TreeSet<>(counts).stream().map(String::valueOf).toList())
                    + " value(s), not " + formulas.size());
        }
        if (name.equals(DrawingVocabulary.TEXT)) {
            return printedText(element, assigned, written, marked);
        }
        Read read = read(name, written, formulas);
        requirePictureWhereDrawn(element, name, read);
        if (read.inputs().stream().anyMatch(texts::contains)) {
            throw refusal("'" + name + "' reads a text input as a number; a text is printed by a glyph's 'text' alone");
        }
        Motion moves = read.value().motion();
        if (moves != marked) {
            String said = name + " = " + written;
            throw refusal(switch (moves) {
                case ANIMATED -> "'" + name + "' moves in time by itself: write 'animate " + said + "'";
                case LIVE -> "'" + name + "' follows " + String.join(", ", read.inputs()) + ": write 'live " + said + "'";
                case STILL -> "'" + said + "' never changes: it is written without '" + marked.name().toLowerCase(Locale.ROOT) + "'";
            });
        }
        return read.value();
    }

    /**
     * A bitmap's {@code pixels} is one picture input, as it is — a row of bytes has no arithmetic —, and a picture is read
     * nowhere else: a number would be read from it.
     */
    private void requirePictureWhereDrawn(ClauseBlockNode element, String name, Read read) {
        boolean drawsPixels = name.equals(DrawingVocabulary.PIXELS);
        boolean onePicture = read.value().expressions().size() == 1
                && read.value().expressions().getFirst() instanceof DrawingExpression.Name(String input) && pictures.contains(input);
        if (drawsPixels && !(element.word().equals(DrawingVocabulary.BITMAP) && onePicture)) {
            throw refusal("'pixels' is a bitmap's, and is one picture input as it is: 'live pixels = screen' with "
                    + "'input screen picture'");
        }
        if (!drawsPixels && read.inputs().stream().anyMatch(pictures::contains)) {
            throw refusal("'" + name + "' reads a picture input as a number; a picture is drawn by a bitmap's 'pixels' alone");
        }
    }

    /**
     * A glyph's {@code text}: words in quotes, printed as they are — a caption the drawing always carries —, or one text
     * input as it is. Either is the part's, so it holds still while the circuit runs and is written without {@code live}.
     */
    private Value printedText(ClauseBlockNode element, Expression assigned, String written, Motion marked) {
        if (!element.word().equals(DrawingVocabulary.GLYPH)) {
            throw refusal("'text' is a glyph's");
        }
        if (marked != Motion.STILL) {
            throw refusal("'text = " + written + "' is the part's and holds still while it runs: write it without '"
                    + marked.name().toLowerCase(Locale.ROOT) + "'");
        }
        if (assigned instanceof WordNode words) {
            return new Value(written, List.of(new DrawingExpression.Words(words.word())), Motion.STILL);
        }
        if (!texts.contains(written)) {
            throw refusal("'text' is one text input as it is — 'text = marking' with 'input marking text' — or words in quotes: "
                    + "'text = \"IC\"'");
        }
        return new Value(written, List.of(new DrawingExpression.Name(written)), Motion.STILL);
    }

    /** A value read: its trees and how they move, and the inputs it follows, for a refusal to name. */
    private record Read(Value value, Set<String> inputs) {
    }

    /** What a value reads, gathered while its trees are made: the inputs it follows, and whether it moves by itself. */
    private static final class Reads {
        private final Set<String> inputs = new TreeSet<>();
        private boolean follows;
        private boolean timed;
    }

    /** A value's formulas read as jMF and turned into trees; it moves as what it reads says. */
    private Read read(String name, String written, List<String> formulas) {
        List<DrawingExpression> trees = new ArrayList<>();
        Reads reads = new Reads();
        for (String formula : formulas) {
            Formula parsed;
            try {
                parsed = LANGUAGE.parse(formula);
            } catch (RuntimeException unreadable) {
                throw refusal("'" + name + " = " + written + "' does not read: " + unreadable.getMessage());
            }
            trees.add(tree(parsed, reads));
        }
        Motion motion = reads.timed ? Motion.ANIMATED : reads.follows ? Motion.LIVE : Motion.STILL;
        return new Read(new Value(written, trees, motion), reads.inputs);
    }

    private DrawingExpression tree(Formula formula, Reads reads) {
        return switch (formula) {
            case Formula.Constant constant -> new DrawingExpression.Number(constant.value());
            case Formula.NamedConstant named -> new DrawingExpression.Number(named.constant().value());
            case Formula.Variable variable -> {
                if (inputs.contains(variable.name())) {
                    reads.follows = true;
                    reads.inputs.add(variable.name());
                } else if (!DrawingVocabulary.WORDS.contains(variable.name())) {
                    throw refusal("'" + variable.name() + "' is neither an input of this drawing ("
                            + (inputs.isEmpty() ? "it declares none" : String.join(", ", new TreeSet<>(inputs)))
                            + ") nor a word it may be drawn in (" + String.join(", ", new TreeSet<>(DrawingVocabulary.WORDS)) + ")");
                }
                yield new DrawingExpression.Name(variable.name());
            }
            case Formula.Negation negation -> new DrawingExpression.Operation("-", List.of(tree(negation.operand(), reads)));
            case Formula.Binary binary -> new DrawingExpression.Operation(binary.operator().symbol(),
                    List.of(tree(binary.left(), reads), tree(binary.right(), reads)));
            case Formula.Comparison comparison -> new DrawingExpression.Operation(comparison.relation().symbol(),
                    List.of(tree(comparison.left(), reads), tree(comparison.right(), reads)));
            case Formula.Logical logical -> new DrawingExpression.Operation(logical.connective().symbol(),
                    List.of(tree(logical.left(), reads), tree(logical.right(), reads)));
            case Formula.Not not -> new DrawingExpression.Operation("not", List.of(tree(not.operand(), reads)));
            case Formula.Piecewise piecewise -> {
                List<DrawingExpression.Choice.Branch> branches = new ArrayList<>();
                for (Formula.Piecewise.Branch branch : piecewise.branches()) {
                    branches.add(new DrawingExpression.Choice.Branch(tree(branch.condition(), reads), tree(branch.value(), reads)));
                }
                DrawingExpression otherwise = piecewise.otherwise() == null ? new DrawingExpression.Name("clear")
                        : tree(piecewise.otherwise(), reads);
                yield new DrawingExpression.Choice(branches, otherwise);
            }
            case Formula.Call call -> {
                String function = call.function().symbol();
                if (!DrawingVocabulary.MATHEMATICS.contains(function)) {
                    throw refusal("'" + function + "' is not drawn: a drawing calls " + String.join(", ",
                            new TreeSet<>(DrawingVocabulary.FUNCTIONS)) + " and " + String.join(", ",
                            new TreeSet<>(DrawingVocabulary.MATHEMATICS)));
                }
                yield call(function, call.arguments(), reads);
            }
            case Formula.HostCall call -> {
                if (DrawingVocabulary.TIMED.contains(call.name())) {
                    reads.timed = true;
                }
                yield call(call.name(), call.arguments(), reads);
            }
            default -> throw refusal("a drawing's value is arithmetic, comparisons, 'if' and calls over its inputs and words");
        };
    }

    private DrawingExpression call(String function, List<Formula> arguments, Reads reads) {
        List<DrawingExpression> read = new ArrayList<>();
        arguments.forEach(argument -> read.add(tree(argument, reads)));
        return new DrawingExpression.Call(function, read);
    }

    /**
     * One binding of a drawing's input — {@code resistance = resistance} — as the tree the browser evaluates: the names it
     * may read are {@code names}, a component's parameters, and the words a drawing is drawn in.
     */
    public static DrawingExpression binding(String what, String input, String expression, Set<String> names) {
        LivingDrawing drawing = new LivingDrawing(what + ": '" + input + " = " + expression + "'");
        drawing.inputs.addAll(names);
        Formula parsed;
        try {
            parsed = LANGUAGE.parse(expression);
        } catch (RuntimeException unreadable) {
            throw drawing.refusal("it does not read: " + unreadable.getMessage());
        }
        return drawing.tree(parsed, new Reads());
    }

    private IllegalArgumentException refusal(String detail) {
        return new IllegalArgumentException(what + ": " + detail + ".");
    }
}
