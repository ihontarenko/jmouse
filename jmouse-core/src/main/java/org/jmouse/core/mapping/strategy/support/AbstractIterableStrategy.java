package org.jmouse.core.mapping.strategy.support;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.*;

abstract public class AbstractIterableStrategy<T> extends AbstractStrategy<T> {

    @Override
    public final T execute(Object source, TypedValue<T> typedValue, MappingContext context) {
        if (source == null) {
            return null;
        }

        ObjectAccessor accessor       = toObjectAccessor(source, context);
        IterableSource iterableSource = toIterableSource(accessor);

        return mapIterable(iterableSource, typedValue, getElementType(typedValue), context);
    }

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

    protected InferredType getElementType(TypedValue<?> typedValue) {
        InferredType type = typedValue.getType();

        if (type.isArray()) {
            return type.getComponentType();
        }

        return type.toCollection().getFirst();
    }

    protected abstract T mapIterable(
            IterableSource iterableSource,
            TypedValue<?> typedValue,
            InferredType elementType,
            MappingContext context
    );

}
