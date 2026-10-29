package org.jmouse.el.grammar;

import org.jmouse.el.lexer.TokenCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The grammar of one kind of line, said as data: its word, the parts that stand after it in order ({@code pad "1"
 * through-hole rectangle}), the clauses that follow in any order, each a word and its value ({@code at 0, 0}, {@code size
 * 1 x 2}, {@code nets vcc, 0}), and a text in quotes that may end it (a path's data). A language says its lines this way;
 * {@link ClauseLineParser} reads any of them, and a line node writes itself back the same way.
 *
 * <p>What a value is, is a {@link ValueKind}: the library's own ({@link Values}) or one a language brings. A new line is
 * one more grammar; a new kind of value one more kind — the reading never changes.</p>
 *
 * @param word        the word the line opens with, or null for a line that opens with a name of its own ({@link #named})
 * @param slots       what stands after the word, in order
 * @param clauses     what follows, each once unless repeatable, in any order
 * @param ending      whether a text in quotes may end the line, and what it is
 * @param endingValue what that text holds: words ({@link Values#TEXT}), or a kind of a language's own
 * @param shape       the line as an author writes it, said in a refusal
 * @param check       a refusal the line's values earn beyond its grammar — a custom pad's outline, a range's order —, or
 *                    null; given the line and its grammar, it answers the refusal's words or null
 * @param term        the word a term writes a line that opens with a name under — {@code pin:out(…)} —; a line that
 *                    opens with a word is written under that word
 * @param termName    the slot a term writes as its name — {@code pad:1(…)} —, or null; a named line's own name always
 * @param asWritten   whether terms keep the line as its grammar writes it: an equation — {@code let x : ratio = …},
 *                    {@code current a -> b = …} — is mathematics, not a term
 * @param prefixes    the words that may stand before the line's word, each saying how it is meant — {@code live glyph …},
 *                    {@code animate opacity = …}; a term writes them as flags
 */
public record LineGrammar(String word, List<Slot> slots, List<Clause> clauses, String ending, ValueKind endingValue,
                          String shape, BiFunction<ClauseLineNode, LineGrammar, String> check, String term,
                          String termName, boolean asWritten, Set<String> prefixes) {

    /**
     * A part standing after the line's word, in order; {@code allowed} empty means any word. A slot with a {@code mark}
     * is written after its word — {@code against b} — and is absent wherever that word does not stand. A slot with a
     * {@code termWord} is written by a term as a named argument — {@code pin: anode} —, where a positional one would read
     * as another slot's.
     */
    public record Slot(String role, ValueKind value, Set<String> allowed, boolean optional, String mark, String termWord) {

        public Slot(String role, ValueKind value, Set<String> allowed, boolean optional, String mark) {
            this(role, value, allowed, optional, mark, null);
        }

        /** The word a term names it by, where it is written as a named argument: its term word, else its mark. */
        public String namedBy() {
            return termWord != null ? termWord : mark;
        }
    }

    /** A clause: its word and its value, or the clauses of its own a {@code nested} grammar says. */
    public record Clause(String word, ValueKind value, Set<String> allowed, boolean required, boolean repeatable,
                         LineGrammar nested) {

        public boolean isNested() {
            return nested != null;
        }
    }

    public LineGrammar {
        slots = List.copyOf(slots);
        clauses = List.copyOf(clauses);
        prefixes = Set.copyOf(prefixes);
    }

    /** How many of its prefixes stand at the cursor, before its word. */
    public int prefixesAt(TokenCursor cursor) {
        int count = 0;
        while (!prefixes.isEmpty() && !Lines.endsAt(cursor, count) && prefixes.contains(cursor.lookAt(count).value())) {
            count++;
        }
        return count;
    }

    public static Builder line(String word) {
        return new Builder(word);
    }

    /**
     * A line that opens with a name of its own — a pin, a part, a parameter —, read as its slot {@code role}; a term
     * writes it as {@code term:name(…)}.
     */
    public static Builder named(String term, String role) {
        Builder builder = new Builder(null).slot(role, Values.NAME);
        builder.term = term;
        builder.termName = role;
        return builder;
    }

    /** The word a term writes the line under: the one it was given for terms, else its own word. */
    public String termWord() {
        return term != null ? term : word;
    }

    /** Whether the line opens with a name rather than a word of the grammar. */
    public boolean isNamed() {
        return word == null;
    }

    public Clause clause(String word) {
        return clauses.stream().filter(clause -> clause.word().equals(word)).findFirst().orElse(null);
    }

    /** The words that open its clauses — where a value before them ends. */
    public Set<String> clauseWords() {
        return Set.copyOf(clauses.stream().map(clause -> clause.word().split("-")[0]).toList());
    }

    /** Builds a grammar a clause at a time. */
    public static final class Builder {

        private final String       word;
        private final List<Slot>   slots   = new ArrayList<>();
        private final List<Clause> clauses = new ArrayList<>();
        private String    ending;
        private ValueKind endingValue = Values.TEXT;
        private BiFunction<ClauseLineNode, LineGrammar, String> check;
        private String    term;
        private String    termName;
        private boolean   asWritten;
        private Set<String> prefixes = Set.of();

        private Builder(String word) {
            this.word = word;
        }

        public Builder slot(String role, ValueKind value, String... allowed) {
            slots.add(new Slot(role, value, Set.of(allowed), false, null));
            return this;
        }

        public Builder optionalSlot(String role, ValueKind value, String... allowed) {
            slots.add(new Slot(role, value, Set.of(allowed), true, null));
            return this;
        }

        /** An optional slot written after its mark: {@code CH2 a against b 1 V/div}. */
        public Builder markedSlot(String role, String mark, ValueKind value) {
            slots.add(new Slot(role, value, Set.of(), true, mark));
            return this;
        }

        /**
         * A slot written after its mark that must stand: {@code W1 wire R1.a}. Right after a named line's name, the mark
         * is what tells the line from another that opens with a name too.
         */
        public Builder requiredMarkedSlot(String role, String mark, ValueKind value) {
            slots.add(new Slot(role, value, Set.of(), false, mark));
            return this;
        }

        /**
         * The word a term writes the line under, where its own word will not do: {@code layer(…)} for {@code live when …},
         * whose {@code when} the lexer keeps as another language's keyword.
         */
        public Builder termed(String word) {
            this.term = word;
            return this;
        }

        /** The words that may stand before the line's word: {@code live}, {@code animate}. */
        public Builder prefixed(String... words) {
            this.prefixes = Set.of(words);
            return this;
        }

        /** Terms keep the line as this grammar writes it: an equation is mathematics, not a term. */
        public Builder writtenAsIs() {
            this.asWritten = true;
            return this;
        }

        /**
         * The last slot is written by a term as the named argument {@code word} — {@code bind:a(pin: anode)} —: a bare
         * name there would read as the slot before it.
         */
        public Builder namedInTerms(String word) {
            Slot last = slots.removeLast();
            slots.add(new Slot(last.role(), last.value(), last.allowed(), last.optional(), last.mark(), word));
            return this;
        }

        /** The slot a term writes as its name: {@code pad:1(…)} for {@code pad "1" …}. */
        public Builder termName(String role) {
            this.termName = role;
            return this;
        }

        public Builder required(String word, ValueKind value, String... allowed) {
            clauses.add(new Clause(word, value, Set.of(allowed), true, false, null));
            return this;
        }

        public Builder optional(String word, ValueKind value, String... allowed) {
            clauses.add(new Clause(word, value, Set.of(allowed), false, false, null));
            return this;
        }

        public Builder repeated(String word, ValueKind value) {
            clauses.add(new Clause(word, value, Set.of(), false, true, null));
            return this;
        }

        /** A clause with clauses of its own, after its word, read until a word of none of them. */
        public Builder nested(String word, LineGrammar nested) {
            clauses.add(new Clause(word, null, Set.of(), false, false, nested));
            return this;
        }

        /** A text in quotes may end the line: {@code role} is what it is ({@code description}). */
        public Builder ending(String role) {
            return ending(role, Values.TEXT);
        }

        /** A text in quotes holding a value of a kind may end the line: {@code role} is what it is ({@code outline}). */
        public Builder ending(String role, ValueKind kind) {
            this.ending = role;
            this.endingValue = kind;
            return this;
        }

        public Builder check(BiFunction<ClauseLineNode, LineGrammar, String> check) {
            this.check = check;
            return this;
        }

        public LineGrammar shaped(String shape) {
            return new LineGrammar(word, slots, clauses, ending, endingValue, shape, check, term, termName, asWritten, prefixes);
        }
    }
}
