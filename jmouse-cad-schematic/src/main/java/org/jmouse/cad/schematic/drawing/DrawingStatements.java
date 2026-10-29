package org.jmouse.cad.schematic.drawing;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.cad.schematic.DrawingGrammar;
import org.jmouse.cad.schematic.drawing.SymbolDrawing.Element;
import org.jmouse.cad.schematic.drawing.SymbolDrawing.Motion;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.ValueKind;
import org.jmouse.el.grammar.Values;
import org.jmouse.el.node.Expression;
import org.jmouse.svg.path.node.PathData;
import org.jmouse.svg.path.parser.PathParseException;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A drawing ({@link SymbolDrawing}) as the statements of its document — its size, its still paths, its inputs, its living
 * elements, its pins, labels and indicators —, the way back from {@link LivingDrawing}: what a drawing canvas's drawing is
 * written with. Every line is built on the grammar that reads it, and every value an author wrote — a range, a property,
 * a layer's condition — is read again by the language from the words the drawing keeps of it.
 */
public final class DrawingStatements {

    /** The classes a canvas draws, each a kind of statement known by its place among its own. */
    public static final String SIZE      = "size";
    public static final String PATH      = "path";
    public static final String UPRIGHT   = "upright-path";
    public static final String PIN       = "pin";
    public static final String LABEL     = "label";
    public static final String INDICATOR = "indicator";
    public static final String INPUT     = "input";
    public static final String ELEMENT   = "element";

    private static final String UPRIGHT_FLAG = "upright";
    private static final String PICTURE_FLAG = "picture";
    private static final String TEXT_FLAG    = "text";
    private static final String GROUP        = "group";

    private static final PathParser PATHS = new PathParser();

    private final DocumentLanguage language;
    private final String           origin;

    private DrawingStatements(DocumentLanguage language, String origin) {
        this.language = language;
        this.origin = origin;
    }

    /**
     * The drawing's statements, in the order a drawing is written: its size, its still paths, its upright ones, its
     * inputs, its elements, its pins, labels and indicators.
     *
     * @param language the language its values are read by
     * @param origin   what a refusal names
     * @throws IllegalArgumentException when a value the drawing keeps does not read, or a line its grammar refuses
     */
    public static List<Expression> of(SymbolDrawing drawing, DocumentLanguage language, String origin) {
        return new DrawingStatements(language, origin).statements(drawing);
    }

    /** A statement's class, as a canvas draws it; empty for one no canvas draws — a {@code meta}, a declaration. */
    public static Optional<String> classOf(Expression statement) {
        return switch (statement) {
            case ClauseBlockNode block when LivingDrawing.isStillPath(block) ->
                    Optional.of(block.header().orElseThrow().has(UPRIGHT_FLAG) ? UPRIGHT : PATH);
            case ClauseBlockNode block when block.header().isPresent() && (DrawingGrammar.SHAPES.contains(block.word())
                    || block.word().equals(DrawingGrammar.LAYER.word())) -> Optional.of(ELEMENT);
            case ClauseLineNode line when line.grammar() == DrawingGrammar.SIZE -> Optional.of(SIZE);
            case ClauseLineNode line when line.grammar() == DrawingGrammar.PIN -> Optional.of(PIN);
            case ClauseLineNode line when line.grammar() == DrawingGrammar.LABEL -> Optional.of(LABEL);
            case ClauseLineNode line when line.grammar() == DrawingGrammar.INDICATOR -> Optional.of(INDICATOR);
            case ClauseLineNode line when line.grammar() == DrawingGrammar.INPUT -> Optional.of(INPUT);
            default -> Optional.empty();
        };
    }

    private List<Expression> statements(SymbolDrawing drawing) {
        List<Expression> statements = new ArrayList<>();
        if (drawing.width() > 0 || drawing.height() > 0) {
            statements.add(LineBuilder.line(DrawingGrammar.SIZE).slot("extent", Nodes.extent(drawing.width(), drawing.height()))
                    .build());
        }
        drawing.paths().forEach(path -> statements.add(stillPath(path, false)));
        drawing.upright().forEach(path -> statements.add(stillPath(path, true)));
        drawing.inputs().forEach(input -> statements.add(input(input)));
        drawing.elements().forEach(element -> statements.add(element(element)));
        for (SymbolDrawing.Pin pin : drawing.pins()) {
            LineBuilder line = LineBuilder.line(DrawingGrammar.PIN).slot("name", Nodes.name(pin.name()))
                    .clause("at", Nodes.point(pin.x(), pin.y()));
            if (pin.label() != null && !pin.label().isEmpty()) {
                line.ending(Nodes.text(pin.label()));
            }
            statements.add(line.build());
        }
        for (SymbolDrawing.Label label : drawing.labels()) {
            statements.add(LineBuilder.line(DrawingGrammar.LABEL).slot("subject", Nodes.word(label.subject()))
                    .clause("at", Nodes.point(label.x(), label.y())).build());
        }
        for (SymbolDrawing.Indicator indicator : drawing.indicators()) {
            statements.add(LineBuilder.line(DrawingGrammar.INDICATOR).slot("name", Nodes.name(indicator.name()))
                    .clause("from", Nodes.point(indicator.left(), indicator.top()))
                    .clause("to", Nodes.point(indicator.right(), indicator.bottom())).build());
        }
        return statements;
    }

    private ClauseBlockNode stillPath(String path, boolean upright) {
        LineBuilder head = LineBuilder.line(DrawingGrammar.element(PATH)).ending(new PathTextNode(pathData(path)));
        if (upright) {
            head.flag(UPRIGHT_FLAG);
        }
        return new ClauseBlockNode(PATH, head.build());
    }

    /**
     * {@code input name [range [typical v]]}, {@code input name picture}, {@code input name text ["typical"]}: a typical
     * value is written only where it is not the range's low end, which is where an unwritten one lies.
     */
    private ClauseLineNode input(SymbolDrawing.Input input) {
        LineBuilder line = LineBuilder.line(DrawingGrammar.INPUT).slot("name", Nodes.name(input.name()));
        if (input.picture()) {
            return line.flag(PICTURE_FLAG).build();
        }
        if (input.text() != null) {
            line.flag(TEXT_FLAG);
            if (!input.text().isEmpty()) {
                line.ending(Nodes.text(input.text()));
            }
            return line.build();
        }
        if (input.range() != null && !input.range().isBlank()) {
            line.slot("range", read(DrawingGrammar.INPUT, Values.RANGE, input.range()));
            if (input.typical() != input.low()) {
                line.clause("typical", Nodes.number(input.typical()));
            }
        }
        return line.build();
    }

    /** An element as its block: its head, then its properties, what a layer hides, and what it holds. */
    private ClauseBlockNode element(Element element) {
        boolean layer = element.shape().equals(DrawingGrammar.LAYER.word());
        LineBuilder head = LineBuilder.line(layer ? DrawingGrammar.LAYER : DrawingGrammar.element(element.shape()));
        if (element.live()) {
            head.prefix(DrawingGrammar.LIVE);
        }
        if (layer) {
            head.slot("condition", read(DrawingGrammar.LAYER, Values.FORMULA, element.condition().written()));
        } else {
            if (element.name() != null && !element.name().isEmpty()) {
                head.slot("name", Nodes.name(element.name()));
            }
            element.geometry().forEach((word, numbers) -> head.clause(word, geometry(word, numbers)));
            if (element.path() != null && !element.path().isEmpty()) {
                head.ending(new PathTextNode(pathData(element.path())));
            }
        }
        ClauseBlockNode block = new ClauseBlockNode(element.shape(), head.build());
        for (Map.Entry<String, SymbolDrawing.Value> property : element.properties().entrySet()) {
            block.addExpression(property(property.getKey(), property.getValue()));
        }
        if (!element.hidden().isEmpty()) {
            block.addExpression(LineBuilder.line(DrawingGrammar.HIDE).slot("names", Nodes.words(element.hidden())).build());
        }
        element.children().forEach(child -> block.addExpression(element(child)));
        if (!block.getExpressions().isEmpty() || layer || element.shape().equals(GROUP)) {
            block.open();
        }
        return block;
    }

    /** {@code [live|animate] name = value}, the value read again from the words it was written in. */
    private ClauseLineNode property(String name, SymbolDrawing.Value value) {
        LineBuilder line = LineBuilder.line(DrawingGrammar.PROPERTY);
        if (value.motion() == Motion.LIVE) {
            line.prefix(DrawingGrammar.LIVE);
        } else if (value.motion() == Motion.ANIMATED) {
            line.prefix(DrawingGrammar.ANIMATE);
        }
        return line.slot("name", Nodes.name(name))
                .slot("value", read(DrawingGrammar.PROPERTY, Values.ASSIGNED, value.written()))
                .build();
    }

    /** {@code size w x h}: an extent; {@code radius r}: one number; {@code at x, y} and the rest: a point. */
    private static Expression geometry(String word, List<Double> numbers) {
        if (numbers.size() == 1) {
            return Nodes.number(numbers.getFirst());
        }
        return word.equals(SIZE) ? Nodes.extent(numbers.get(0), numbers.get(1)) : Nodes.point(numbers.get(0), numbers.get(1));
    }

    private Expression read(LineGrammar line, ValueKind kind, String written) {
        if (written == null || written.isBlank()) {
            throw new IllegalArgumentException(origin + ": a " + line.termWord() + " line is given no value");
        }
        return language.value(line, kind, Set.of(), written, origin);
    }

    private PathData pathData(String written) {
        try {
            return PATHS.parse(origin, written == null ? "" : written);
        } catch (PathParseException refused) {
            throw new IllegalArgumentException(origin + ": the path data does not read: " + refused.detail());
        }
    }
}
