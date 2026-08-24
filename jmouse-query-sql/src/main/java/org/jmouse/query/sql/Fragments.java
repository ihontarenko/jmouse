package org.jmouse.query.sql;

import org.jmouse.jdbc.parameters.ListParameterSource;
import org.jmouse.jdbc.parameters.ParameterSource;

/**
 * The stitch between a compiled query and the JDBC layer that runs it.
 *
 * <p>⚠️ <strong>One method, and that is the measure of how well the two halves already fit.</strong> A
 * {@link Fragment} is SQL plus positional values; a {@link ListParameterSource} is positional values for
 * SQL. Nothing has to be converted, renamed or reordered — the seam is a hand-off rather than an
 * adapter, which is what it should look like when a language and an execution layer were designed to the
 * same idea of what a parameter is.</p>
 *
 * <p>⚠️ Note what this class is <em>not</em>: it does not execute anything, hold a connection, or know a
 * template exists. Keeping it that thin is what lets {@code jmouse-query-sql} be depended on by
 * something that assembles SQL for a purpose of its own.</p>
 *
 * <p>It is also deliberately separate from {@link org.jmouse.jdbc.parameters.SQLPlan}, which answers a
 * different question: that describes <em>where the named placeholders are</em> in a piece of SQL, while
 * a fragment carries <em>the values</em> for positional ones. Two records that look alike and are not.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Fragments {

    private Fragments() {
    }

    /**
     * The fragment's values, as the JDBC layer's positional source.
     *
     * @param fragment a compiled fragment
     * @return its parameters, ready to bind
     */
    public static ParameterSource parameters(Fragment fragment) {
        return new ListParameterSource(fragment.parameters());
    }
}
