package org.jmouse.mapper.strategy.converted;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.binding.TypeMappingRule;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;

import java.util.List;

/**
 * Contributes {@link ConvertedPairStrategy} for a pair some rule converts whole. 🎁
 *
 * <h2>⚠️ Priority</h2>
 *
 * <p>{@code Integer.MIN_VALUE}, because a whole-target rule has to beat every shape-based contributor:
 * the target of {@code from BigDecimal : via("money")} is a bean, a record or a scalar like any other,
 * and each of those would happily claim it and map it property by property. The next value up,
 * {@code MIN_VALUE + 1}, is already the bean strategy, so there is no room between them — which is why
 * this sits at the floor rather than one step above it.</p>
 *
 * <p>⚠️ It shares that floor with {@code TypeMapperStrategyContributor}, which is <strong>not</strong>
 * in the default contributor set and is added by a caller who wrote a {@code TypeMapper} by hand. Where
 * both are present <em>and both claim the same pair</em>, the sort is stable so registration order
 * decides. That is a genuine conflict rather than an ordering detail — a product has said the same
 * thing twice, in Java and in a file — and it is left visible rather than resolved by a number nobody
 * would think to look at.</p>
 *
 * <h2>⚠️ This runs once per pair, not once per object</h2>
 *
 * <p>{@code supports} consults the rule registry, which reads as an expensive thing to put in front of
 * every other contributor. It is not: the strategy registry caches its answer per
 * {@code (sourceClass, targetType)}, so a contributor is asked only while a pair is being decided for
 * the first time. Nothing here is on the per-object path.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(Integer.MIN_VALUE)
public final class ConvertedPairStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return rule(source, targetType, context) != null;
    }

    @Override
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return new ConvertedPairStrategy<>(rule(source, typedValue.getType(), context));
    }

    /**
     * The whole-target rule for this pair, if one is declared.
     *
     * <p>⚠️ The <strong>first</strong> such rule wins, and more than one is not looked for. A target
     * type is described in one file, so two whole-target rules for one pair means two files claiming
     * the same target — which the reader already refuses when it loads them. Searching for a second
     * here would be a check in the wrong place, reported at the wrong time.</p>
     *
     * @param source     the object being mapped, whose runtime class is the source type
     * @param targetType what it is being mapped into
     * @param context    the mapping context, which holds the rule registry
     * @return the rule, or {@code null} when this pair is mapped property by property
     */
    private TypeMappingRule rule(Object source, InferredType targetType, MappingContext context) {
        if (source == null || targetType == null) {
            return null;
        }

        List<TypeMappingRule> rules =
                context.mappingRegistry().find(source.getClass(), targetType.getClassType(), context);

        for (TypeMappingRule rule : rules) {
            if (rule.mapsWhole()) {
                return rule;
            }
        }

        return null;
    }
}
