package org.jmouse.query.sql;

/**
 * What {@code view "…" on inventory} actually means — the table the rows come from, and what to call it.
 *
 * <h2>⚠️ Deliberately pure data: it renders nothing</h2>
 *
 * <p>An earlier version carried {@code from()}, {@code column()} and {@code qualifiedKey()} helpers, and
 * they produced <strong>unquoted</strong> identifiers because a record has no dialect to ask. The result
 * was a statement quoting some names and not others — which works until a column is called {@code order}
 * or {@code key}, and then fails in production rather than in a test.</p>
 *
 * <p>So rendering lives on {@link SqlContext}, which holds the dialect. This stays four strings.</p>
 *
 * <h2>Why a record and not a mapping document</h2>
 *
 * <p>The obvious next thought is to declare this the way {@code .jmp} declares a policy — a
 * {@code source inventory { from entries key id … }} block. It is very likely where this ends up.</p>
 *
 * <p>⚠️ It is deliberately <strong>not</strong> where it starts. There is not yet a single real product
 * mapping in existence, and a declarative format designed against one imagined consumer is a format that
 * fits nobody. {@code .jmp} reads well because it was written after somebody knew what needed declaring.
 * When two products have written a mapping, the shape they share is worth a grammar — and that grammar
 * will compile <em>into</em> this rather than replacing it.</p>
 *
 * @param name  what a document calls it — {@code inventory}
 * @param table the table the rows live in — {@code entries}
 * @param alias what that table is called in the statement — {@code e}
 * @param key   the column another table joins against — {@code id}
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record QueryTarget(String name, String table, String alias, String key) {
}
