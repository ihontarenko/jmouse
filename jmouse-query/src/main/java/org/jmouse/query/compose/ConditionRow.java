package org.jmouse.query.compose;

/**
 * One row of a filter builder — the shape a screen draws and the shape a query is composed from.
 *
 * <h2>⚠️ This is the only thing an interface sends, and it holds no syntax</h2>
 *
 * <p>No pipe, no quotes, no {@code is not}, no parentheses. A row says <em>which attribute</em>,
 * <em>which comparison</em>, <em>what value</em>, and whether rows <em>missing that attribute
 * altogether</em> should come along. Everything else — the converter an ordered comparison needs, how a
 * value is spelled, where the brackets go — is decided by {@link QueryComposer} against the schema.</p>
 *
 * <p>An interface that spelled any of it would be a second implementation of this language, and the two
 * would agree for about a month. That is not a hypothetical: a browser-side writer and a browser-side
 * reader drifted far enough to turn {@code submitter == currentMember} into a comparison against the
 * <em>word</em> {@code currentMember} — silently, matching nothing, refusing nothing.</p>
 *
 * @param attribute      exactly as the schema names it — {@code entry[quantity]}, {@code created}
 * @param operator       a name from {@link RowOperators}
 * @param value          what to compare against, as a plain value; {@code null} where the operator needs
 *                       none. ⚠️ Never a fragment of source: a row cannot smuggle syntax in here, because
 *                       whatever arrives becomes a <strong>literal</strong> and nothing else
 * @param includeMissing whether rows that have no such attribute at all should also match — only
 *                       meaningful for a negative comparison, and ignored otherwise
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ConditionRow(String attribute, String operator, Object value, boolean includeMissing) {

    public ConditionRow(String attribute, String operator, Object value) {
        this(attribute, operator, value, false);
    }
}
