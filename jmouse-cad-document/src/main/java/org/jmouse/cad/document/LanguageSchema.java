package org.jmouse.cad.document;

import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.ClauseLineParser;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.LineSet;
import org.jmouse.el.grammar.ValueKind;
import org.jmouse.el.grammar.Values;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The jM language as an editor builds its forms from it: every kind and the blocks its body may hold, every block's
 * head and the lines it holds, every line's word, its slots and clauses, the words each may be, and the kind of value
 * each takes. Read off the grammar the reader reads with ({@link LanguageWalk}), never listed by hand, so a form cannot
 * offer what the reader refuses.
 *
 * @param kinds  every kind, by its word's order
 * @param blocks every block, in the order the modules bring them — {@code meta} first, which every document may hold
 */
public record LanguageSchema(List<Kind> kinds, List<Block> blocks) {

    /** Where the names of the value kinds are declared: the grammar library's, then the document language's own. */
    private static final List<Class<?>> VALUE_HOLDERS = List.of(Values.class, DocumentValues.class);

    public LanguageSchema {
        kinds = List.copyOf(kinds);
        blocks = List.copyOf(blocks);
    }

    /**
     * A kind of document.
     *
     * @param blocks     the blocks its body may hold, {@code meta} among them
     * @param repeatable the blocks of those it may hold more than once
     * @param lines      a drawn kind's body lines — a symbol's {@code pin}, a footprint's {@code pad} —; empty otherwise
     */
    public record Kind(String word, Set<String> blocks, Set<String> repeatable, List<Line> lines) {
    }

    /** What a block's body is: lines of a grammar, a jMS script, or {@code meta}'s free fields. */
    public enum BlockForm { LINES, SCRIPT, META }

    /**
     * A block.
     *
     * @param header the line its head is, when it is headed — {@code symbol uses …} —; null otherwise
     * @param lines  the lines its body holds, for {@link BlockForm#LINES}
     */
    public record Block(String word, BlockForm form, Line header, List<Line> lines) {
    }

    /**
     * One line of a grammar; a line read by something other than a grammar — an equation — has only {@code says}.
     *
     * @param term     the word a term spelling opens with — {@code pin} for {@code pin:a(…)} —, or null
     * @param prefixes the words that may stand before its word, each saying how it is meant
     * @param says     what the line is, in the reader's own words — what a refusal names it by
     */
    public record Line(String word, String term, List<Slot> slots, List<Clause> clauses, String ending,
                       String endingValue, Set<String> prefixes, String says) {
    }

    /**
     * A part standing after the line's word, in order.
     *
     * @param value   the kind of value: {@code word}, {@code quantity}, {@code point}, {@code path}…
     * @param allowed the only words it may be; empty for any
     * @param mark    the word it is written after — {@code against b} —, or null
     */
    public record Slot(String role, String value, Set<String> allowed, boolean optional, String mark, String termWord) {
    }

    /** A clause: its word and its value — or, nested, a line of its own. */
    public record Clause(String word, String value, Set<String> allowed, boolean required, boolean repeatable,
                         Line nested) {
    }

    /** The schema of the language the modules make. */
    public static LanguageSchema of(List<DocumentModule> modules) {
        LanguageWalk walk = LanguageWalk.of(modules);
        Map<ValueKind, String> names = valueNames();
        List<Kind> kinds = new ArrayList<>();
        walk.kinds().forEach((word, kind) -> {
            Set<String> blocks = new TreeSet<>(kind.blocks());
            blocks.add(MetaParser.WORD);
            LineSet drawn = walk.drawn().get(word);
            kinds.add(new Kind(word, blocks, new TreeSet<>(kind.repeatable()),
                               drawn == null ? List.of() : lines(drawn, names)));
        });
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(MetaParser.WORD, BlockForm.META, null, List.of()));
        for (ClauseBlockParser block : walk.blocks().values()) {
            blocks.add(new Block(block.word(), BlockForm.LINES, block.header().map(header -> line(header, names)).orElse(null),
                                 block.lines() == null ? List.of() : lines(block.lines(), names)));
        }
        walk.scripts().forEach(script -> blocks.add(new Block(script, BlockForm.SCRIPT, null, List.of())));
        return new LanguageSchema(kinds, blocks);
    }

    private static List<Line> lines(LineSet set, Map<ValueKind, String> names) {
        List<Line> lines = new ArrayList<>();
        for (LineReader reader : set.readers()) {
            for (LineReader alternative : reader.alternatives()) {
                lines.add(alternative instanceof ClauseLineParser parser ? line(parser.grammar(), names)
                                  : new Line(null, null, List.of(), List.of(), null, null, Set.of(), alternative.says()));
            }
        }
        return lines;
    }

    private static Line line(LineGrammar grammar, Map<ValueKind, String> names) {
        List<Slot> slots = grammar.slots().stream()
                .map(slot -> new Slot(slot.role(), name(slot.value(), names, grammar), new TreeSet<>(slot.allowed()),
                                      slot.optional(), slot.mark(), slot.termWord()))
                .toList();
        List<Clause> clauses = grammar.clauses().stream()
                .map(clause -> new Clause(clause.word(), clause.isNested() ? null : name(clause.value(), names, grammar),
                                          new TreeSet<>(clause.allowed()), clause.required(), clause.repeatable(),
                                          clause.isNested() ? line(clause.nested(), names) : null))
                .toList();
        return new Line(grammar.word(), grammar.termWord(), slots, clauses, grammar.ending(),
                        grammar.endingValue() == null ? null : name(grammar.endingValue(), names, grammar),
                        new TreeSet<>(grammar.prefixes()), grammar.shape());
    }

    private static String name(ValueKind kind, Map<ValueKind, String> names, LineGrammar grammar) {
        if (kind == null) {
            return null;
        }
        String name = names.get(kind);
        if (name == null) {
            throw new IllegalStateException("A value of the line '" + grammar.word() + "' is of a kind no holder names: "
                                            + "declare it as a constant of " + VALUE_HOLDERS.getLast().getSimpleName());
        }
        return name;
    }

    /** Every declared value kind, by the name its constant spells in lowercase words: {@code EXTENT_OR_QUANTITY}, {@code extent-or-quantity}. */
    private static Map<ValueKind, String> valueNames() {
        Map<ValueKind, String> names = new IdentityHashMap<>();
        for (Class<?> holder : VALUE_HOLDERS) {
            for (Field field : holder.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && ValueKind.class.isAssignableFrom(field.getType())) {
                    try {
                        names.put((ValueKind) field.get(null), field.getName().toLowerCase(Locale.ROOT).replace('_', '-'));
                    } catch (IllegalAccessException unreachable) {
                        throw new IllegalStateException(unreachable);
                    }
                }
            }
        }
        return names;
    }
}
