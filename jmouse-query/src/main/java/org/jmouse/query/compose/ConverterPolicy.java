package org.jmouse.query.compose;

import org.jmouse.query.schema.QueryAttribute;

/**
 * Which converter an ordered comparison over a given attribute needs — the product's rule, stated once.
 *
 * <h2>⚠️ The schema knows a value is untyped; only the product knows what it really holds</h2>
 *
 * <p>{@link QueryAttribute#needsConverterForOrdering()} answers <em>a converter is required here</em>.
 * It cannot answer <em>which one</em>, because that is a fact about the store: a bag holding
 * {@code "3300|mΩ"} is read with {@code int} in one product and might be {@code double} in another.</p>
 *
 * <h2>⚠️ One policy feeds the schema a screen is given AND the query it composes</h2>
 *
 * <p>That is the whole reason this is an argument rather than a constant. The converter a builder shows
 * beside a field and the converter the composed query carries are the same answer from the same object,
 * so they cannot disagree — and they did disagree, in a browser, where one of them was a string
 * template.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface ConverterPolicy {

    /**
     * ⚠️ Everything is typed, so nothing needs converting. Correct for a schema whose attributes are all
     * declared — and quietly wrong for one with a bag in it, where the refusal will name the attribute.
     */
    ConverterPolicy NONE = attribute -> null;

    /**
     * @param attribute the attribute being compared
     * @return the converter's name — {@code int}, {@code double} — or {@code null} where none is needed
     */
    String converterFor(QueryAttribute attribute);
}
