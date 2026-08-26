package org.jmouse.mapper.el.builder;

import java.util.List;

/**
 * A mapping as a form fills it in — rows, not text. 🧾
 *
 * <h2>⚠️ This is what a browser posts, and it is deliberately not a string</h2>
 *
 * <p>A screen that builds a mapping could assemble {@code "reference : reference | trim | upper"} and
 * send that. It must not, and the reason is not tidiness: the moment a browser writes the language,
 * there are <strong>two implementations of it</strong> — and the first thing a second writer gets wrong
 * is quoting. A name holding a quote, a value holding a newline, a filter argument holding a comma:
 * each of those is a rule somebody has to know, and knowing it in two places means knowing it
 * differently within a month.</p>
 *
 * <p>So a form posts structure and the server renders it, through the same translator an editor saves
 * through. The draft below mirrors the document's own node model one for one, which is what makes that
 * rendering a walk rather than a translation.</p>
 *
 * @param name    what the document calls itself
 * @param imports the types it names, fully qualified
 * @param targets what it builds
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record MappingDraft(String name, List<String> imports, List<TargetDraft> targets) {

    /**
     * One target type and everything that fills it.
     *
     * @param type    the target, as the document names it
     * @param always  rules that apply whatever the source is
     * @param sources the source types it is built from
     */
    public record TargetDraft(String type, List<MappingRow> always, List<SourceDraft> sources) {
    }

    /**
     * One source type and the rules that read it.
     *
     * @param type  the source, as the document names it
     * @param rules what it fills
     */
    public record SourceDraft(String type, List<MappingRow> rules) {
    }

    /**
     * One row of the form — one rule.
     *
     * <h3>⚠️ {@code ignored} is a field rather than a value in {@code expression}</h3>
     *
     * <p>{@code ignore} is the one thing on the right of a rule that is never evaluated, so a row
     * carrying the literal text {@code "ignore"} in its expression would be indistinguishable from a
     * row whose source path happens to be called that. The form has a checkbox; the draft has a
     * boolean.</p>
     *
     * @param target     the target property this row fills
     * @param expression what produces the value, as written — {@code null} where the row is ignored
     * @param condition  a trailing {@code when}, or {@code null}
     * @param ignored    whether the property is deliberately not carried
     */
    public record MappingRow(String target, String expression, String condition, boolean ignored) {

        /**
         * A row that fills a property.
         *
         * @param target     the target property
         * @param expression what produces the value
         * @return the row
         */
        public static MappingRow of(String target, String expression) {
            return new MappingRow(target, expression, null, false);
        }

        /**
         * A row that fills a property only when a condition holds.
         *
         * <p>⚠️ A false condition writes <strong>nothing</strong> — it does not write null. The two are
         * different outcomes and look identical in a passing test, which is why the form has to offer
         * the condition rather than let somebody spell it as a ternary.</p>
         *
         * @param target     the target property
         * @param expression what produces the value
         * @param condition  when to write it
         * @return the row
         */
        public static MappingRow when(String target, String expression, String condition) {
            return new MappingRow(target, expression, condition, false);
        }

        /**
         * A row that deliberately carries nothing.
         *
         * @param target the target property
         * @return the row
         */
        public static MappingRow ignored(String target) {
            return new MappingRow(target, null, null, true);
        }
    }
}
