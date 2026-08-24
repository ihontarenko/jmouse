package org.jmouse.query.spring.builder;

import org.jmouse.query.compose.ConditionRow;

import java.util.List;

/**
 * What travels between a builder and the server.
 *
 * <h2>⚠️ Two endpoints, and the second one answers everything</h2>
 *
 * <p>A screen asks what it may name, and then — for every change a person makes — asks the server to
 * translate it. That second call carries the verdict, the composed text and the rows all at once,
 * because a screen needs all three and they must agree. Splitting them into a compose call, a parse call
 * and a check call would mean three chances to disagree and three round trips.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class QueryViews {

    private QueryViews() {
    }

    /**
     * One thing a query may name.
     *
     * @param name      exactly as a query writes it
     * @param label     what a person calls it
     * @param type      {@code text} · {@code number} · {@code boolean} · {@code temporal} · {@code unknown}
     * @param access    {@code column} · {@code bag} · {@code joined} · {@code collection}
     * @param converter ⚠️ the converter an ordered comparison needs, or {@code null}. Shown so a screen
     *                  can explain the pipe it will see in the text; the screen never has to place it
     * @param options   the choices a closed set offers
     */
    public record Attribute(
            String name,
            String label,
            String type,
            String access,
            String converter,
            List<String> options) {
    }

    /**
     * @param subject    which listing this describes
     * @param attributes what may be named
     * @param operators  ⚠️ the comparisons a row may use, sent rather than hard-coded in a screen — a
     *                   builder offering an operator the composer does not have is a refusal a person
     *                   cannot understand
     */
    public record Vocabulary(String subject, List<Attribute> attributes, List<Operator> operators) {
    }

    /**
     * @param spelling   what a row sends
     * @param needsValue whether to draw a value control at all
     * @param ordered    whether it is an ordered comparison
     * @param negative   whether the <em>and those missing it entirely?</em> switch belongs beside it
     */
    public record Operator(String spelling, boolean needsValue, boolean ordered, boolean negative) {
    }

    /**
     * What a screen asks to have translated.
     *
     * <h2>⚠️ Rows or filter, never both</h2>
     *
     * <p>Which one is present says which way the translation goes: rows mean <em>compose this</em>, text
     * means <em>read this back</em>. Sending both would leave the server choosing which of two things the
     * person meant, and it would choose wrongly at exactly the moment they disagree.</p>
     *
     * @param rows       the builder's rows, when the builder is what changed
     * @param filter     the written query, when the text is what changed
     * @param orderBy    the attribute to sort by, or blank
     * @param descending which way
     */
    public record Translation(
            List<ConditionRow> rows,
            String filter,
            String orderBy,
            Boolean descending) {

        /**
         * ⚠️ {@code Boolean} rather than {@code boolean}, and normalised here.
         *
         * <p>A primitive component makes the field <strong>required</strong>: a body of
         * {@code {"filter": "…"}} is refused outright with <em>the request body could not be read</em>,
         * which says nothing about which field was missing. That is a poor way for a shared endpoint to
         * meet any caller but the one interface that happens to send every field.</p>
         */
        public Translation {
            descending = descending != null && descending;
        }

        /** Whether a sort was asked for at all. */
        public boolean descends() {
            return Boolean.TRUE.equals(descending);
        }
    }

    /**
     * Everything a screen needs after a change, in one answer.
     *
     * @param filter   the query as jMQ — ⚠️ written by the AST, so it is exactly what will run
     * @param order    the sort as jMQ, or blank
     * @param rows     the rows a builder should draw, or {@code null} meaning ⚠️ <strong>this cannot be
     *                 drawn</strong> — a supplied value, an expression, an {@code or}. The screen then
     *                 says so and hands over the text rather than approximating it
     * @param readable whether it makes sense against the schema
     * @param message  the refusal in the checker's own words, or {@code null}
     */
    public record Translated(
            String filter,
            String order,
            List<ConditionRow> rows,
            boolean readable,
            String message) {
    }
}
