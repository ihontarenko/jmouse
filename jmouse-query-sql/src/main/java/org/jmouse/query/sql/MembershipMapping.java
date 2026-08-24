package org.jmouse.query.sql;

import org.jmouse.query.schema.QueryAttribute;

import java.util.List;

/**
 * How a collection is <em>asked about</em> — the only thing that can be done with one.
 *
 * <h2>⚠️ A second interface rather than a case inside {@link AttributeMapping}</h2>
 *
 * <p>{@code AttributeMapping} answers <em>"what does this attribute read as"</em>, and a collection has
 * no answer to that: it is many rows, not a value. Forcing one would mean returning a joined column, and
 * a row with three labels would then come back three times — the one failure the collection declaration
 * exists to make impossible.</p>
 *
 * <p>So the question is different and the interface is different. A mapping that cannot answer it is
 * simply not asked, and the compiler refuses by name instead of inventing something plausible.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface MembershipMapping {

    /** What is being asked of a collection. */
    enum Question {

        /** {@code is hasAny([…])} — at least one of these. */
        ANY,

        /** {@code is hasAll([…])} — every one of these. */
        ALL,

        /** {@code is hasNone([…])} — not one of these. */
        NONE
    }

    /**
     * The condition answering one question about one collection.
     *
     * @param attribute the collection attribute, as the schema declared it
     * @param question  what is being asked
     * @param items     the values asked about, already compiled and carrying their bound parameters
     * @param context   the compile in progress
     * @return a boolean SQL condition
     */
    Fragment membership(QueryAttribute attribute, Question question, List<Fragment> items, SqlContext context);
}
