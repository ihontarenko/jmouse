package org.jmouse.core.mapping.plan;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;

/**
 * Strategy interface for resolving a {@link MappingPlan} for a mapping request. 🧠
 *
 * <p>A {@code PlanRegistry} selects (and optionally caches) an appropriate mapping plan based on:</p>
 * <ul>
 *   <li>the runtime source object</li>
 *   <li>the requested target {@link TypedValue} (type metadata + optional instance)</li>
 *   <li>the current {@link MappingContext}</li>
 * </ul>
 *
 * <p>Implementations typically delegate plan construction to one or more contributors/factories.</p>
 *
 * @see MappingPlan
 * @see MappingContext
 */
public interface PlanRegistry {

    /**
     * Resolve a mapping plan for the given mapping request.
     *
     * @param source source object
     * @param typedValue target typed value (type metadata + optional instance holder)
     * @param context mapping context
     * @param <T> plan output type
     * @return mapping plan for the request
     */
    <T> MappingPlan<T> planFor(Object source, TypedValue<T> typedValue, MappingContext context);
}
