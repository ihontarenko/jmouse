package org.jmouse.mapper.strategy.converted;

import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.binding.MappingAssertion;
import org.jmouse.mapper.binding.MappingAssertions;
import org.jmouse.mapper.binding.TypeMappingRule;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.strategy.support.AbstractStrategy;

/**
 * A pair converted whole — one expression whose result <em>is</em> the target. 🎁
 *
 * <p>A value object, a money type, an identifier wrapper: the target is not assembled from properties,
 * it is produced in one step. So this strategy constructs nothing, fills nothing, and writes nothing —
 * it evaluates the rule's expression and hands back what it returned.</p>
 *
 * <h2>⚠️ A strategy rather than a branch in the mapper</h2>
 *
 * <p>The obvious implementation tests for a whole-target rule inside {@code ObjectMapper.map} and skips
 * the strategy when it finds one. That puts a lookup on the hottest path in the engine for a construct
 * almost no pair uses, and it makes "which code runs for this pair" a question with two answers. The
 * engine already resolves a strategy per pair and caches it; a pair converted whole is simply a
 * different strategy, decided once.</p>
 *
 * <h2>⚠️ What does and does not run, and why</h2>
 *
 * <table border="1">
 *   <caption>Composition with the rest of the mapping</caption>
 *   <tr><th>Construct</th><th>Here</th></tr>
 *   <tr><td>{@code refuse source before}</td>
 *       <td><strong>runs</strong> — it is about the source, and the source is unchanged by any of
 *           this</td></tr>
 *   <tr><td>{@code refuse target before}</td>
 *       <td><strong>never runs</strong>, and is refused when the file is read. It fires only for a
 *           caller-supplied instance, and a caller-supplied instance is discarded here — so it would
 *           be a check on an object that is about to be thrown away</td></tr>
 *   <tr><td>{@code refuse target after}</td>
 *       <td><strong>runs</strong>, against whatever the expression produced</td></tr>
 *   <tr><td>a caller-supplied instance</td>
 *       <td>⚠️ <strong>discarded</strong> — the expression produces the object, so there is nothing to
 *           write into. Refused when the file is read rather than silently ignored at runtime</td></tr>
 *   <tr><td>{@code unmapped fail}</td>
 *       <td>meaningless — nothing is filled property by property. Refused when the file is read</td></tr>
 *   <tr><td>the null-handling policy</td>
 *       <td>does not apply: it governs writing a property, and no property is written</td></tr>
 * </table>
 *
 * @param <T> the target type
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ConvertedPairStrategy<T> extends AbstractStrategy<T> {

    private final TypeMappingRule rule;

    public ConvertedPairStrategy(TypeMappingRule rule) {
        this.rule = rule;
    }

    /**
     * Produces the target by evaluating the rule's expression against the source.
     *
     * @param source     the object being mapped
     * @param typedValue the target descriptor
     * @param context    the mapping context
     * @return whatever the expression produced, adapted to the target type
     */
    @Override
    @SuppressWarnings("unchecked")
    public T execute(Object source, TypedValue<T> typedValue, MappingContext context) {
        MappingAssertions.refuse(rule.assertions(), MappingAssertion.Subject.SOURCE,
                                 MappingAssertion.Phase.BEFORE, source,
                                 ErrorCodes.REFUSE_SOURCE, context);

        Object produced = rule.whole().function().compute(source, context);

        // ⚠️ Adapted, not cast. The expression may legitimately produce something the conversion layer
        // can carry the rest of the way — a String for an enum target, a BigDecimal for a double — and
        // refusing that here would make the whole-pair form stricter than every rule beside it.
        Object target = adaptValue(produced, typedValue.getType(), context);

        MappingAssertions.refuse(rule.assertions(), MappingAssertion.Subject.TARGET,
                                 MappingAssertion.Phase.AFTER, target,
                                 ErrorCodes.REFUSE_TARGET, context);

        return (T) target;
    }
}
