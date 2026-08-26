package org.jmouse.mapper.strategy.support;

import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.MappingDestination;
import org.jmouse.mapper.config.CollectionMappingPolicy;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.core.reflection.InferredType;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Base strategy for mapping iterable sources into {@link Collection} targets. 🧺
 *
 * <p>This strategy builds a target collection via a factory provided by subclasses and then
 * iterates over the source, mapping each element into the requested element type.</p>
 *
 * <p>When the target already exists - mapping into a bean's current collection, say -
 * {@link CollectionMappingPolicy} decides whether the source replaces its contents or is appended
 * to them. Under the default {@link CollectionMappingPolicy#REPLACE} the target is cleared first,
 * so mapping the same source twice cannot double its elements.</p>
 *
 * <p>Collection growth is guarded by {@code context.config().maxCollectionSize()} to prevent
 * runaway allocations (e.g., accidentally mapping an infinite/very large source).</p>
 *
 * @see AbstractIterableStrategy
 */
public abstract class AbstractCollectionStrategy extends AbstractIterableStrategy<Collection<Object>> {

    /**
     * Map the provided iterable source into the target collection.
     *
     * <p>Elements are adapted using {@link #adaptValue(Object, TypedValue, MappingContext, MappingDestination)}.</p>
     *
     * @param iterableSource iterable source adapter
     * @param typedValue typed target descriptor (may carry an existing collection)
     * @param elementType inferred element type for target collection
     * @param context mapping context
     * @return populated target collection
     * @throws org.jmouse.mapper.errors.MappingException if the number of elements exceeds
     *         {@code maxCollectionSize}
     */
    @Override
    protected final Collection<Object> mapIterable(
            IterableSource iterableSource,
            TypedValue<?> typedValue,
            InferredType elementType,
            MappingContext context
    ) {
        Collection<Object> target  = getTargetCollection(typedValue, context);
        int                maximum = context.config().maxCollectionSize();
        int                index   = 0;

        for (var iterator = iterableSource.iterator(); iterator.hasNext(); ) {
            if (index >= maximum) {
                throw toMappingException(
                        context,
                        ErrorCodes.COLLECTION_SIZE_EXCEEDS,
                        "Collection size exceeds maxCollectionSize=" + maximum,
                        null
                );
            }

            MappingContext     elementContext = context.appendPath("[" + index + "]");
            MappingDestination destination    = pluginsActive(elementContext)
                    ? new MappingDestination.CollectionElement(
                            target, elementContext.currentPath(), index, elementType)
                    : null;

            target.add(adaptValue(iterator.next(), TypedValue.of(elementType), elementContext, destination));
            index++;
        }

        return target;
    }

    /**
     * Resolve the target {@link Collection} instance to be populated.
     *
     * <p>An instance carried by {@link TypedValue} is reused, so in-place mapping keeps the very
     * collection the target object already holds. Under {@link CollectionMappingPolicy#REPLACE} it
     * is cleared before use; under {@link CollectionMappingPolicy#MERGE_APPEND} its contents are
     * kept and the source is appended.</p>
     *
     * @param typedValue typed target descriptor that may carry an existing collection instance
     * @param context mapping context
     * @return target collection instance (never {@code null})
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> getTargetCollection(TypedValue<?> typedValue, MappingContext context) {
        Collection<Object> collection = (Collection<Object>) typedValue.getValue().get();

        if (collection == null) {
            return getCollectionFactory(context).get();
        }

        if (context.policy().collectionMappingPolicy() == CollectionMappingPolicy.REPLACE) {
            collection.clear();
        }

        return collection;
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
