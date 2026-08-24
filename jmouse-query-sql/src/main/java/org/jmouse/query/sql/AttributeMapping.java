package org.jmouse.query.sql;

import org.jmouse.query.schema.QueryAttribute;

/**
 * How one product's attribute becomes SQL — the seam that lets one compiler serve two shapes of data.
 *
 * <h2>⚠️ Why this cannot live in the library</h2>
 *
 * <p>The library knows an attribute is reached as a {@code COLUMN} or out of a {@code BAG}, because the
 * schema said so. It does <strong>not</strong> know that the bag is a table called {@code field_entries},
 * joined on {@code entry_id}, keyed by a column called {@code field}, with the value in
 * {@code text_value} — and it must not, because the next product spells all four differently.</p>
 *
 * <p>So the compiler asks this, once per attribute reference, and gets back an SQL <em>expression</em>
 * it can then compare, convert or sort. Any join the answer needs is registered on the context along the
 * way.</p>
 *
 * <h2>⚠️ A bag access must allocate its own alias</h2>
 *
 * <p>One row of a bag cannot be both the name and the quantity. Two attributes sharing a join alias
 * therefore ask for a single row to equal two different things, and the query returns nothing where an
 * {@code and} was meant — while looking entirely correct on data small enough to check by eye.
 * {@link SqlContext#alias(String)} hands out one alias per key and reuses it, which is what makes
 * repeating the same attribute cheap and mixing two attributes safe.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface AttributeMapping {

    /**
     * The SQL expression this attribute reads as, together with any values that expression binds.
     *
     * <h2>⚠️ It returns a {@link Fragment} and not a {@code String}, deliberately</h2>
     *
     * <p>A bare string has nowhere to put a bound value, so an expression needing one — a JSON path, a
     * discriminator, anything parameterised — would have to be written into the SQL text. That is a
     * concatenated query, and it is exactly the mistake this cluster refused to make when it kept the
     * compiler out of {@code toSource()}.</p>
     *
     * <p>It matters most <em>here</em> because this is the one method a <strong>product</strong>
     * implements. Every other step in the compiler carries its values as a matter of course; an
     * inconsistency in the one place outside the library's control is the inconsistency that gets used.</p>
     *
     * @param attribute what the schema said about it
     * @param context   where to allocate an alias or register a join
     * @return the SQL expression and the values it binds, in order
     */
    Fragment expression(QueryAttribute attribute, SqlContext context);
}
