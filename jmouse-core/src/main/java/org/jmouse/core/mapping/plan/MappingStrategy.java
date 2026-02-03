package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.MappingContext;

/**
 * Compiled mapping unit responsible for producing a target value from a source object. 🧠
 *
 * <p>A {@code MappingStrategy} encapsulates the concrete mapping algorithm for a specific
 * source/target type combination.</p>
 *
 * <p>Plans are typically created by {@link MappingPlanContributor}s and resolved through a
 * {@link StrategyRegistry}.</p>
 *
 * @param <T> target type produced by this plan
 */
public interface MappingStrategy<T> {

    /**
     * Execute mapping from the given {@code source} into a target value.
     *
     * @param source source object (may be {@code null}, depending on plan behavior)
     * @param context mapping context providing accessors, conversion, configuration, policies, etc.
     * @return mapped target value
     */
    T execute(Object source, MappingContext context);
}
