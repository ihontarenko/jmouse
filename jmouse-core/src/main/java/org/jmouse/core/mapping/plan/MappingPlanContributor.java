package org.jmouse.core.mapping.plan;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

/**
 * Factory/selector for building {@link MappingStrategy} instances. 🧩
 *
 * <p>A {@code MappingPlanContributor} participates in plan resolution by:</p>
 * <ol>
 *   <li>declaring support for a mapping request via {@link #supports(Object, InferredType, MappingContext)}</li>
 *   <li>building a concrete {@link MappingStrategy} via {@link #build(TypedValue, MappingContext)}</li>
 * </ol>
 *
 * <p>The {@link TypedValue} provided to {@link #build(TypedValue, MappingContext)} may carry:</p>
 * <ul>
 *   <li>the inferred target type</li>
 *   <li>an existing target instance for in-place mapping</li>
 *   <li>a supplier for lazy instance creation</li>
 * </ul>
 *
 * <p>Contributors are typically registered in a {@link StrategyRegistry} and consulted in order
 * until a compatible contributor is found.</p>
 *
 * @see MappingStrategy
 * @see StrategyRegistry
 * @see TypedValue
 */
public interface MappingPlanContributor {

    /**
     * Determine whether this contributor can build a mapping plan for the given request.
     *
     * @param source source object (may be {@code null})
     * @param targetType inferred target type
     * @param context mapping context
     * @return {@code true} if supported, otherwise {@code false}
     */
    boolean supports(Object source, InferredType targetType, MappingContext context);

    /**
     * Build a {@link MappingStrategy} for the given typed target value.
     *
     * @param typedValue typed target descriptor (type metadata + optional instance holder)
     * @param context mapping context
     * @param <T> plan output type
     * @return mapping plan instance
     */
    <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context);
}
