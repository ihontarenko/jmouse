package org.jmouse.query.model;

import java.util.List;

/**
 * A view as plain data — what a builder loads, and what an "explain this" endpoint returns.
 *
 * <h2>⚠️ A PROJECTION. Read-only, always derived, never stored</h2>
 *
 * <p>This is decision 1 continued, not an exception to it. A view is stored as <strong>one</strong>
 * thing — its text — and the moment this could be saved there would be two records of one fact: a
 * hand-edited document would leave a stored model lying, and nothing would notice.</p>
 *
 * <p>So it is regenerated from the text every time, and there is deliberately <em>no</em> endpoint,
 * constructor or method anywhere that turns one back into a document. <strong>People edit the text; this
 * is always downstream of it.</strong></p>
 *
 * <h2>What it buys</h2>
 *
 * <ul>
 *   <li>⚠️ A builder loads JSON and <strong>needs no parser on the interface side</strong> — the editor
 *       package is then only highlighting.</li>
 *   <li>A backend that is not SQL can read this instead of walking an AST.</li>
 *   <li>There is something to answer <em>"what does this view actually do?"</em> with.</li>
 * </ul>
 *
 * @param title    what the view is called
 * @param target   what it is about
 * @param where    the condition, as text — ⚠️ the language's own spelling, so it can be shown or re-parsed
 * @param columns  what it returns
 * @param group    what it gathers by, empty when it does not
 * @param having   the group condition, or {@code null}
 * @param order    how it is sorted
 * @param grouped  whether a row of the result is a tuple rather than a row of the underlying thing
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record QueryModel(String title, String target, String where, List<Projection> columns,
                         List<String> group, String having, List<Sort> order, boolean grouped) {

    /**
     * One returned value.
     *
     * @param expression what is returned, as text
     * @param alias      what it is called, or {@code null}
     */
    public record Projection(String expression, String alias) {
    }

    /**
     * One sort key.
     *
     * @param expression what to sort by, as text
     * @param descending ⚠️ whether it runs backwards. A boolean rather than the written {@code asc}, so a
     *                   caller does not have to know that omitting the word means ascending.
     */
    public record Sort(String expression, boolean descending) {
    }
}
