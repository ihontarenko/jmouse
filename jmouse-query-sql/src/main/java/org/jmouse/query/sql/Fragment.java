package org.jmouse.query.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * A piece of SQL and the values that go with it, in the order a statement will bind them.
 *
 * <p>⚠️ <strong>The two halves travel together because they are one fact.</strong> A compile step that
 * returned only a {@code String} would have nowhere to put a value, so it would have to write the value
 * into the text — and that is a concatenated query, which is the injection surface typed parameters
 * exist to remove. Returning the pair means nothing is ever concatenated, which is a stronger position
 * than escaping carefully.</p>
 *
 * @param sql        the SQL, holding {@code ?} where each parameter goes
 * @param parameters the values, in the order the {@code ?} appear
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record Fragment(String sql, List<Object> parameters) {

    /** Nothing at all — the identity of {@link #then}. */
    private static final Fragment EMPTY = new Fragment("", List.of());

    public Fragment {
        parameters = List.copyOf(parameters);
    }

    public static Fragment of(String sql, Object... parameters) {
        return new Fragment(sql, List.of(parameters));
    }

    public static Fragment empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return sql.isBlank();
    }

    /**
     * Puts another fragment after this one, keeping the parameters in the same order as the text.
     *
     * <p>⚠️ <strong>This ordering is the whole reason fragments are assembled rather than concatenated
     * as strings.</strong> Parameters bind by position, so text and values have to be appended in step.
     * A join written into the SQL before the {@code WHERE} whose parameter was appended after the
     * {@code WHERE}'s produces a query that runs, returns rows, and answers a question nobody asked.</p>
     *
     * @param other     what follows
     * @param separator what goes between them when both hold something
     * @return the two, in order
     */
    public Fragment then(Fragment other, String separator) {
        if (other == null || other.isEmpty()) {
            return this;
        }

        if (isEmpty()) {
            return other;
        }

        List<Object> combined = new ArrayList<>(parameters);

        combined.addAll(other.parameters);

        return new Fragment(sql + separator + other.sql, combined);
    }

    /** Wraps the SQL, leaving the parameters exactly where they were. */
    public Fragment wrap(String before, String after) {
        return isEmpty() ? this : new Fragment(before + sql + after, parameters);
    }

    /**
     * Puts this fragment into a template, once per {@code %s}, repeating its parameters to match.
     *
     * <h2>⚠️ Why this cannot be {@code template.formatted(sql)}</h2>
     *
     * <p>Some SQL has to name a value <strong>more than once</strong> — a guarded cast tests the value
     * and then casts it, so {@code %s} appears twice. Substituting the text twice while binding the
     * values once produces a statement with more placeholders than parameters.</p>
     *
     * <p>Against a bare column that is harmless, because a column reference binds nothing. It breaks the
     * moment the value is itself an expression: {@code entry[x] | before("|") | int} put a {@code ?} in
     * the cast's argument, and the statement asked for seven values while six were supplied. ⚠️ It
     * raised — this time. A template repeating a value <em>twice</em> where the caller had bound
     * <em>two</em> parameters would have run and bound them to the wrong slots.</p>
     *
     * @param template SQL holding one or more {@code %s} where this fragment's SQL goes
     * @return the filled template, with this fragment's parameters repeated once per occurrence
     */
    public Fragment substitute(String template) {
        int occurrences = template.split("%s", -1).length - 1;

        if (occurrences <= 0) {
            return new Fragment(template, List.of());
        }

        Object[] copies = new Object[occurrences];
        List<Object> repeated = new ArrayList<>();

        for (int index = 0; index < occurrences; index++) {
            copies[index] = sql;
            repeated.addAll(parameters);
        }

        return new Fragment(template.formatted(copies), repeated);
    }

    /**
     * Both conditions, parenthesised and joined with {@code AND}.
     *
     * <p>⚠️ <strong>This is how a condition the caller owns is put onto one the document wrote</strong> —
     * a tenant restriction, a workspace scope, a soft-delete filter. It matters that it lives here and
     * not in each caller: parenthesising both sides is what stops an {@code OR} inside either one from
     * swallowing the other, and a caller assembling {@code a + " AND " + b} by hand gets that wrong
     * exactly once, silently, and widens what a person may see.</p>
     *
     * <p>An empty side is not a condition, so it disappears rather than producing a dangling operator.</p>
     *
     * @param other the condition to add
     * @return both, or whichever is not empty
     */
    public Fragment and(Fragment other) {
        if (other == null || other.isEmpty()) {
            return this;
        }

        if (isEmpty()) {
            return other;
        }

        return wrap("(", ")").then(other.wrap("(", ")"), " AND ");
    }
}
