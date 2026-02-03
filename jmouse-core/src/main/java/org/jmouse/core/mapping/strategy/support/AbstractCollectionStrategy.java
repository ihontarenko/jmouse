package org.jmouse.core.mapping.strategy.support;

import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Base plan for mapping iterable sources into {@link Collection} targets. 🧺
 *
 * <p>This plan builds a target collection via a factory provided by subclasses and then
 * iterates over the source, mapping each element into the requested element type.</p>
 *
 * <p>Collection growth is guarded by {@code context.config().maxCollectionSize()} to prevent
 * runaway allocations (e.g., accidentally mapping an infinite/very large source).</p>
 *
 * @see AbstractIterableStrategy
 */
public abstract class AbstractCollectionStrategy extends AbstractIterableStrategy<Collection<Object>> {

    /**
     * Map the provided iterable source into a newly created target collection.
     *
     * <p>Elements are adapted using {@link #adaptValue(Object, InferredType, MappingContext)}.</p>
     *
     * @param iterableSource iterable source adapter
     * @param elementType inferred element type for target collection
     * @param context mapping context
     * @return populated target collection
     * @throws IllegalStateException if the number of elements exceeds {@code maxCollectionSize}
     */
    @Override
    protected final Collection<Object> mapIterable(IterableSource iterableSource, InferredType elementType, MappingContext context) {
        Collection<Object> target  = getCollectionFactory(context).get();
        int                maximum = context.config().maxCollectionSize();
        int                counter = 0;

        for (var iterator = iterableSource.iterator(); iterator.hasNext(); ) {
            if (++counter > maximum) {
                throw new IllegalStateException("Collection size exceeds maxCollectionSize=" + maximum);
            }
            MappingContext mappingContext = context.appendPath("[" + (counter - 1) + "]");
            target.add(adaptValue(iterator.next(), elementType, mappingContext));
        }

        return target;
    }

    /**
     * Provide a factory for creating the target collection instance.
     *
     * <p>Subclasses typically select the concrete collection implementation based on the
     * requested target type and/or configuration.</p>
     *
     * @param context mapping context
     * @return supplier that creates a new target collection instance
     */
    protected abstract Supplier<Collection<Object>> getCollectionFactory(MappingContext context);
}
