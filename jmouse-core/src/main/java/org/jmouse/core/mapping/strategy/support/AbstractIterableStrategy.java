package org.jmouse.core.mapping.strategy.support;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.*;

/**
 * Base strategy for mapping iterable-like sources (arrays, collections, iterables) into iterable targets. 🔁
 *
 * <p>{@code AbstractIterableStrategy} normalizes the source into an {@link IterableSource} abstraction
 * and delegates the actual materialization to {@link #mapIterable(IterableSource, TypedValue, InferredType, MappingContext)}.</p>
 *
 * <p>Two source shapes are supported:</p>
 * <ul>
 *   <li>{@link Iterable}: wrapped into an {@link IterableSource} with unknown size</li>
 *   <li>indexed sources (arrays/collections exposed via {@link ObjectAccessor#length()}): wrapped into an indexed
 *       {@link IterableSource} with known size and cheap random access</li>
 * </ul>
 *
 * @param <T> target type produced by this strategy
 */
abstract public class AbstractIterableStrategy<T> extends AbstractStrategy<T> {

    /**
     * Execute iterable mapping by normalizing the source into {@link IterableSource}.
     *
     * @param source source value (may be {@code null})
     * @param typedValue typed target descriptor
     * @param context mapping context
     * @return mapped value, or {@code null} when {@code source} is {@code null}
     */
    @Override
    public final T execute(Object source, TypedValue<T> typedValue, MappingContext context) {
        if (source == null) {
            return null;
        }

        ObjectAccessor accessor       = toObjectAccessor(source, context);
        IterableSource iterableSource = toIterableSource(accessor);

        return mapIterable(iterableSource, typedValue, getElementType(typedValue), context);
    }

    /**
     * Adapt an {@link ObjectAccessor} into an {@link IterableSource}.
     *
     * <p>If the accessor wraps an {@link Iterable}, an iterator-only source is created (unknown size).
     * Otherwise an indexed source is created using {@link ObjectAccessor#length()} and {@link ObjectAccessor#get(int)}.</p>
     *
     * @param accessor source accessor
     * @return iterable source adapter
     */
    private IterableSource toIterableSource(ObjectAccessor accessor) {
        if (accessor.isIterable() && accessor.unwrap() instanceof Iterable<?> iterable) {
            return new IterableSource() {

                @Override
                public Iterator<?> iterator() {
                    return iterable.iterator();
                }

                @Override
                public OptionalInt knownSize() {
                    return OptionalInt.empty();
                }

                @Override
                public String toString() {
                    return "IterableSource: Unknown size";
                }

            };
        }

        int size = accessor.length();

        return new IterableSource() {
            @Override
            public Iterator<?> iterator() {
                return new Iterator<>() {

                    int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < size;
                    }

                    @Override
                    public Object next() {
                        return accessor.get(index++).unwrap();
                    }

                };
            }

            @Override
            public OptionalInt knownSize() {
                return OptionalInt.of(size);
            }

            @Override
            public Object get(int index) {
                return accessor.get(index).unwrap();
            }

            @Override
            public boolean isIndexed() {
                return true;
            }

            @Override
            public String toString() {
                return "IterableSource: " + size + " size";
            }

        };
    }

    /**
     * Resolve the element type for an iterable target described by {@link TypedValue}.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>for array targets: use {@link InferredType#getComponentType()}</li>
     *   <li>for collection targets: use {@code type.toCollection().getFirst()}</li>
     * </ul>
     *
     * @param typedValue typed target descriptor
     * @return inferred element type
     */
    protected InferredType getElementType(TypedValue<?> typedValue) {
        InferredType type = typedValue.getType();

        if (type.isArray()) {
            return type.getComponentType();
        }

        return type.toCollection().getFirst();
    }

    /**
     * Materialize the target value from the normalized {@link IterableSource}.
     *
     * @param iterableSource iterable source adapter
     * @param typedValue typed target descriptor (may include existing target instance)
     * @param elementType inferred element type of the target
     * @param context mapping context
     * @return mapped target value
     */
    protected abstract T mapIterable(
            IterableSource iterableSource,
            TypedValue<?> typedValue,
            InferredType elementType,
            MappingContext context
    );

}
