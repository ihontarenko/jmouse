package org.jmouse.el.grammar;


import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.writer.SourceWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A line read by its {@link LineGrammar}: what stands after its word by role, its clauses in the order written, and the
 * text that ends it. It writes itself back in the same order.
 */
public class ClauseLineNode extends AbstractExpression {

    /** A clause as written: its word, and its value — none for a flag. */
    public record Written(String word, Expression value) {
    }

    private final LineGrammar             grammar;
    private final boolean                 headed;
    private final Map<String, Expression> slots;
    private final List<Written>           clauses;
    private final Expression              ending;
    private final List<String>            prefixes;
    private final int                     endingAt;

    /**
     * @param headed whether it writes its word: a nested clause's value does not, its clause word stands before it
     * @param ending the text in quotes that ends it — its words, or its path data's tree —, or null
     */
    public ClauseLineNode(LineGrammar grammar, boolean headed, Map<String, Expression> slots, List<Written> clauses, Expression ending) {
        this(grammar, headed, List.of(), slots, clauses, ending);
    }

    /** @param prefixes the words written before its word, in order: {@code live}, {@code animate} */
    public ClauseLineNode(LineGrammar grammar, boolean headed, List<String> prefixes, Map<String, Expression> slots,
                          List<Written> clauses, Expression ending) {
        this(grammar, headed, prefixes, slots, clauses, ending, clauses.size());
    }

    /**
     * @param endingAt how many of its clauses stand before the text that ends it — {@code path "M…" upright} writes its
     *                 data before its flag —; by default all of them
     */
    public ClauseLineNode(LineGrammar grammar, boolean headed, List<String> prefixes, Map<String, Expression> slots,
                          List<Written> clauses, Expression ending, int endingAt) {
        this.grammar = grammar;
        this.headed = headed;
        this.prefixes = List.copyOf(prefixes);
        this.slots = Collections.unmodifiableMap(new LinkedHashMap<>(slots));
        this.clauses = List.copyOf(clauses);
        this.ending = ending;
        this.endingAt = Math.min(endingAt, this.clauses.size());
    }

    /** How many of its clauses stand before the text that ends it. */
    public int endingAt() {
        return endingAt;
    }

    /** The words written before its word: {@code live} of {@code live glyph …}. */
    public List<String> prefixes() {
        return prefixes;
    }

    /**
     * The same line without what its author wrote above it, keeping what stands beside it: what a reader that hands the
     * comments above a line to the block it gathers the line into leaves of the line.
     */
    public ClauseLineNode withoutLeadingTrivia() {
        ClauseLineNode copy = new ClauseLineNode(grammar, headed, prefixes, slots, clauses, ending, endingAt);
        copy.setTrailingTrivia(getTrailingTrivia());
        return copy;
    }

    /**
     * The same line as a block's head, which writes neither its word nor its prefixes — the block writes them before
     * it —, keeping what is written above and beside it; itself when it is one already.
     */
    public ClauseLineNode asHeader() {
        if (!headed) {
            return this;
        }
        ClauseLineNode copy = new ClauseLineNode(grammar, false, prefixes, slots, clauses, ending, endingAt);
        copy.addLeadingTrivia(getLeadingTrivia());
        copy.setTrailingTrivia(getTrailingTrivia());
        return copy;
    }

    public String word() {
        return grammar.word();
    }

    public LineGrammar grammar() {
        return grammar;
    }

    public Optional<Expression> slot(String role) {
        return Optional.ofNullable(slots.get(role));
    }

    /** A slot's word, or a text's words. */
    public Optional<String> name(String role) {
        return slot(role).map(value -> ((WordNode) value).word());
    }

    /** Whether a clause — or a prefix — is written: {@code filled}, {@code live}. */
    public boolean has(String clause) {
        return prefixes.contains(clause) || clauses.stream().anyMatch(written -> written.word().equals(clause));
    }

    public Optional<Expression> value(String clause) {
        return clauses.stream().filter(written -> written.word().equals(clause)).findFirst().map(Written::value);
    }

    /** Every value of a repeatable clause, in order: a track's {@code to}. */
    public List<Expression> values(String clause) {
        List<Expression> found = new ArrayList<>();
        clauses.stream().filter(written -> written.word().equals(clause)).forEach(written -> found.add(written.value()));
        return found;
    }

    public Optional<PointNode> point(String clause) {
        return value(clause).map(PointNode.class::cast);
    }

    public Optional<Double> quantity(String clause) {
        return value(clause).map(value -> ((QuantityLiteralNode) value).getQuantity().value());
    }

    /**
     * The quantities of a slot or a clause that holds a list of them, each in SI: a board's {@code sizes tracks 0.25, 0.4},
     * its {@code drill 0.3, 0.4}. Empty when the line writes none.
     */
    public List<Double> quantities(String name) {
        return value(name).or(() -> slot(name))
                .map(value -> ((ListNode) value).items().stream()
                        .map(item -> ((QuantityLiteralNode) item).getQuantity().value()).toList())
                .orElse(List.of());
    }

    public Optional<String> word(String clause) {
        return value(clause).map(value -> ((WordNode) value).word());
    }

    /** The words of a clause that holds a list: a net class's {@code nets}. */
    public List<String> names(String clause) {
        return value(clause).map(ClauseLineNode::wordsOf).orElse(List.of());
    }

    /**
     * The words of a slot that holds a list: a keepout's {@code layers}, a layer's {@code hide} names. ⚠️ Not
     * {@link #names}, which reads a clause of that name and finds nothing where a slot stands.
     */
    public List<String> slotNames(String role) {
        return slot(role).map(ClauseLineNode::wordsOf).orElse(List.of());
    }

    private static List<String> wordsOf(Expression value) {
        return ((ListNode) value).items().stream().map(item -> ((WordNode) item).word()).toList();
    }

    public Optional<ClauseLineNode> nested(String clause) {
        return value(clause).map(ClauseLineNode.class::cast);
    }

    /** The text that ends it, as it says: its words, or the value its kind read written back. */
    public Optional<String> ending() {
        return Optional.ofNullable(ending).map(value -> value instanceof WordNode word ? word.word() : value.toString());
    }

    /** The value that ends it, as its kind read it: a {@link WordNode} for words, a language's own node otherwise. */
    public Optional<Expression> endingValue() {
        return Optional.ofNullable(ending);
    }

    public List<Written> clauses() {
        return clauses;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        if (headed) {
            prefixes.forEach(writer::word);
            writer.word(grammar.word());
        }
        for (LineGrammar.Slot slot : grammar.slots()) {
            Expression value = slots.get(slot.role());
            if (value != null) {
                writer.word(slot.mark()).write(value);
            }
        }
        for (int index = 0; index < clauses.size(); index++) {
            if (index == endingAt && ending != null) {
                writer.write(ending);
            }
            Written written = clauses.get(index);
            writer.word(written.word());
            if (written.value() != null) {
                writer.write(written.value());
            }
        }
        if (ending != null && endingAt >= clauses.size()) {
            writer.write(ending);
        }
    }
}
