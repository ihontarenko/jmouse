package org.jmouse.validator.el.builder;

import java.util.List;
import java.util.Map;

/**
 * A validation document as a form fills it in — rows, not text. 🧾
 *
 * <h2>⚠️ This is what a browser posts, and it is deliberately not a string</h2>
 *
 * <p>A screen building a validation could assemble {@code "part_number : size(3, 32)"} and send that. It
 * must not, and the reason is not tidiness: the moment a browser writes the language there are
 * <strong>two implementations of it</strong> — and the first thing a second writer gets wrong is
 * quoting. A message holding an apostrophe, a pattern holding a backslash, an argument holding a comma:
 * each is a rule somebody has to know, and knowing it in two places means knowing it differently within
 * a month.</p>
 *
 * <p>So a form posts structure and the server renders it, through the same writer an editor saves
 * through. The draft below mirrors the document's own node model one for one, which is what makes that
 * rendering a walk rather than a translation.</p>
 *
 * <h2>⚠️ It is a tree, not a table</h2>
 *
 * <p>A flat list of rows was the obvious shape and is the wrong one. jMV says outright that
 * {@code when a { when b { … } }} and {@code when a and b { … }} are the same document and that neither
 * is canonical — so a form that could only show the flat one would refuse a file the language calls
 * idiomatic, and a form that flattened on the way in would rewrite somebody's file for them.</p>
 *
 * @param name     what the document calls itself
 * @param comments the file's header, line by line — see {@link ItemDraft} for why these travel
 * @param items    everything the document holds, in the order it was written
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ValidationDraft(String name, List<String> comments, List<ItemDraft> items) {

    public ValidationDraft {
        comments = comments == null ? List.of() : List.copyOf(comments);
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * One thing a document holds: a block, a check line, a guard, or an invariant.
     *
     * <h3>⚠️ One record with a kind, because JSON has no unions</h3>
     *
     * <p>Four records and a discriminator field would be the same thing spelled longer, and a separate
     * list per kind — which is how the mapping draft avoids this — cannot be used here: it would lose
     * the <em>order</em> statements were written in, and order decides the order faults are reported
     * in.</p>
     *
     * <h3>⚠️ Comments travel with the row</h3>
     *
     * <p>Without them, a document opened in the form and saved comes back stripped of every explanation
     * somebody wrote — the exact loss the trivia layer was built to stop, re-introduced at the one seam
     * that matters most. The form shows them; the form keeps them.</p>
     *
     * @param kind      what this row is
     * @param comments  what was written above it, line by line, {@code #} included
     * @param note      what was written after it on its own line, or {@code null}
     * @param block     {@code gate} or {@code always} — only for {@link Kind#BLOCK}
     * @param field     the field a line is about — only for {@link Kind#LINE}
     * @param checks    what is asked of it, in order — only for {@link Kind#LINE}
     * @param message   a line's message, or an invariant's
     * @param condition a guard's condition, or an invariant's assertion
     * @param items     a block's contents, or a guard's guarded branch
     * @param otherwise a guard's other branch — ⚠️ {@code null} where none was written, which is not the
     *                  same as an empty one: {@code otherwise { }} says somebody considered the other
     *                  case and decided nothing applies
     */
    public record ItemDraft(
            Kind             kind,
            List<String>     comments,
            String           note,
            String           checksNote,
            String           block,
            String           field,
            List<CheckDraft> checks,
            String           message,
            String           condition,
            List<ItemDraft>  items,
            List<ItemDraft>  otherwise
    ) {

        /** What a row is. */
        public enum Kind { BLOCK, LINE, GUARD, INVARIANT }

        public ItemDraft {
            comments = comments == null ? List.of() : List.copyOf(comments);
            checks = checks == null ? List.of() : List.copyOf(checks);
            items = items == null ? List.of() : List.copyOf(items);
            otherwise = otherwise == null ? null : List.copyOf(otherwise);
        }

        /**
         * A {@code gate} or an {@code always} block.
         *
         * @param block which one
         * @param items what stands in it
         * @return the row
         */
        public static ItemDraft block(String block, List<ItemDraft> items) {
            return new ItemDraft(Kind.BLOCK, null, null, null, block, null, null, null, null, items, null);
        }

        /**
         * A check line.
         *
         * @param field   what it is about
         * @param checks  what is asked, in order
         * @param message the line's message, or {@code null}
         * @return the row
         */
        public static ItemDraft line(String field, List<CheckDraft> checks, String message) {
            return new ItemDraft(Kind.LINE, null, null, null, null, field, checks, message, null, null, null);
        }

        /**
         * A guard.
         *
         * @param condition when it applies
         * @param items     what applies while it holds
         * @param otherwise what applies while it does not, or {@code null} where none was written
         * @return the row
         */
        public static ItemDraft guard(String condition, List<ItemDraft> items,
                                      List<ItemDraft> otherwise) {
            return new ItemDraft(Kind.GUARD, null, null, null, null, null, null, null, condition, items,
                                 otherwise);
        }

        /**
         * An assertion about the record.
         *
         * @param condition what must hold
         * @param message   what to say when it does not
         * @return the row
         */
        public static ItemDraft invariant(String condition, String message) {
            return new ItemDraft(Kind.INVARIANT, null, null, null, null, null, null, message, condition,
                                 null, null);
        }

        /**
         * The same row, carrying what was written around it.
         *
         * @param comments   what stood above it
         * @param note       what stood after it, or {@code null}
         * @param checksNote what stood after its checks where a message continues below them
         * @return a new row; this one is unchanged
         */
        public ItemDraft with(List<String> comments, String note, String checksNote) {
            return new ItemDraft(kind, comments, note, checksNote, block, field, checks, message,
                                 condition, items, otherwise);
        }
    }

    /**
     * One check on one field.
     *
     * <h3>⚠️ Arguments travel as they were written</h3>
     *
     * <p>An argument is an expression — {@code min(other_field)} and {@code max(quantity - reserved)}
     * are things somebody will write — so what the form edits is its <em>text</em>. Evaluating it here,
     * or re-quoting it, would be the browser deciding what a rule means.</p>
     *
     * @param check      the word the file writes — {@code size}, {@code oneOf}, {@code required}
     * @param positional its arguments by position, each as written
     * @param named      its arguments by name, each as written
     * @param stop       whether a failure here silences the rest of this field's checks
     * @param message    what to say when it fails, or {@code null} to take the line's
     */
    public record CheckDraft(
            String              check,
            List<String>        positional,
            Map<String, String> named,
            boolean             stop,
            String              message
    ) {

        public CheckDraft {
            positional = positional == null ? List.of() : List.copyOf(positional);
            named = named == null ? Map.of() : Map.copyOf(named);
        }

        /**
         * A check taking arguments by position.
         *
         * @param check      the word the file writes
         * @param positional its arguments, as written
         * @return the check
         */
        public static CheckDraft of(String check, String... positional) {
            return new CheckDraft(check, List.of(positional), Map.of(), false, null);
        }

        /**
         * The same check, silencing the rest of its field on failure.
         *
         * @return a new check; this one is unchanged
         */
        public CheckDraft stopping() {
            return new CheckDraft(check, positional, named, true, message);
        }

        /**
         * The same check, with something to say.
         *
         * @param message what to say when it fails
         * @return a new check; this one is unchanged
         */
        public CheckDraft saying(String message) {
            return new CheckDraft(check, positional, named, stop, message);
        }
    }
}
