package org.jmouse.cad.simulation;

import org.jmouse.cad.DocumentHead;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Given;
import org.jmouse.cad.Index;
import org.jmouse.cad.Parameter;
import org.jmouse.cad.Point;
import org.jmouse.cad.Range;
import org.jmouse.cad.Setting;
import org.jmouse.cad.Text;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.el.grammar.AssignedNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ConnectionsNode;
import org.jmouse.el.grammar.BlockNode;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.FormulaNode;
import org.jmouse.el.grammar.ListNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.el.grammar.NameNode;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.grammar.RangeNode;
import org.jmouse.el.grammar.SettingsNode;
import org.jmouse.el.grammar.ToleranceNode;
import org.jmouse.el.grammar.WordNode;
import org.jmouse.el.node.Expression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * How the values a document's lines hold become the model's: an expression a {@link Formula}, a text a {@link Text}, a
 * range, a point, settings, connections. Every reading of a simulated document goes through here, so a value is
 * understood one way wherever it stands.
 */
final class ModelValues {

    private ModelValues() {
    }

    static DocumentHead head(DocumentNode document) {
        Map<String, String> meta = new LinkedHashMap<>();
        document.block(MetaNode.class).ifPresent(block -> block.lines().forEach(line -> meta.put(line.key(), text(line))));
        return new DocumentHead(document.slug(), document.title(), document.type().orElse(null), meta);
    }

    private static String text(MetaLineNode line) {
        return line.group().isEmpty() ? line.text() : String.join("\n", line.group());
    }

    /** The lines of a block, each read by its grammar; a document without the block has none. */
    static List<ClauseLineNode> lines(DocumentNode document, String word) {
        return document.block(word).map(ModelValues::lines).orElse(List.of());
    }

    static List<ClauseLineNode> lines(BlockNode block) {
        return block.getExpressions().stream().filter(ClauseLineNode.class::isInstance).map(ClauseLineNode.class::cast).toList();
    }

    /** The name a slot holds, without its index: {@code q} of {@code q[0..7]}. */
    static String base(ClauseLineNode line, String role) {
        return ((NameNode) line.slot(role).orElseThrow()).base();
    }

    /** The index a slot's name is written with, or null for a name without one. */
    static Index index(ClauseLineNode line, String role) {
        NameNode name = (NameNode) line.slot(role).orElseThrow();
        if (!name.indexed()) {
            return null;
        }
        return name.range().map(range -> new Index(range[0], range[1])).orElseGet(() -> Index.counted(name.count().orElseThrow()));
    }

    static Given given(Expression value) {
        return switch (value) {
            case AssignedNode assigned -> given(assigned.value());
            case WordNode word -> new Text(word.word());
            case FormulaNode formula -> new Formula(formula.written());
            case ToleranceNode tolerance -> new Formula(tolerance.amount().written());
            default -> throw new IllegalArgumentException("'" + value + "' is no value");
        };
    }

    static Formula formula(Expression value) {
        Given given = given(value);
        if (given instanceof Formula formula) {
            return formula;
        }
        throw new IllegalArgumentException("'" + value + "' is a text where an expression is written");
    }

    static Optional<Formula> formula(ClauseLineNode line, String clause) {
        return line.value(clause).map(ModelValues::formula);
    }

    static Optional<Formula> slotFormula(ClauseLineNode line, String role) {
        return line.slot(role).map(ModelValues::formula);
    }

    static Range range(Expression value) {
        RangeNode range = (RangeNode) value;
        if (range.listed().isPresent()) {
            return new Range.Listed(new Formula(range.listed().get().written()));
        }
        return new Range.Between(new Formula(range.low().orElseThrow().written()), new Formula(range.high().orElseThrow().written()));
    }

    static Point point(Expression value) {
        PointNode point = (PointNode) value;
        return new Point(point.x(), point.y());
    }

    static Optional<Point> point(ClauseLineNode line, String clause) {
        return line.value(clause).map(ModelValues::point);
    }

    static List<Setting> settings(Expression value) {
        return ((SettingsNode) value).settings().stream()
                .map(setting -> new Setting(setting.name(), setting.given().map(ModelValues::given).orElse(null))).toList();
    }

    static List<Connection> connections(Expression value) {
        return ((ConnectionsNode) value).connections().stream()
                .map(connection -> new Connection(connection.pin(), connection.net())).toList();
    }

    /** Groups of words or numbers — {@code 1 7}, {@code 15, 1, 2}, {@code clock edge, cs bubble} —, as written. */
    static List<List<String>> groups(Expression value) {
        return ((ListNode) value).items().stream()
                .map(group -> ((ListNode) group).items().stream().map(item -> ((WordNode) item).word()).toList()).toList();
    }

    static Parameter parameter(ClauseLineNode line) {
        Set<String> flags = new LinkedHashSet<>();
        SimulationVocabulary.PARAMETER_FLAGS.stream().filter(line::has).forEach(flags::add);
        return new Parameter(line.name("name").orElseThrow(), given(line.slot("value").orElseThrow()),
                line.value("in").map(ModelValues::range).orElse(null), formula(line, "step").orElse(null),
                line.word("per").orElse(null), flags, line.ending().orElse(""));
    }

    static List<Parameter> parameters(DocumentNode document) {
        return lines(document, "parameters").stream().map(ModelValues::parameter).toList();
    }
}
