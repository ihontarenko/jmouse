package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.*;

abstract public class AbstractIterablePlan<T> extends AbstractPlan<T> {

    protected AbstractIterablePlan(InferredType targetType) {
        super(targetType);
    }

    @Override
    public final T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        ObjectAccessor accessor       = toObjectAccessor(source, context);
        IterableSource iterableSource = toIterableSource(accessor);

        return mapIterable(iterableSource, getElementType(), context);
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

    protected InferredType getElementType() {
        InferredType type = getTargetType();

        if (type.isArray()) {
            return type.getComponentType();
        }

        return type.toCollection().getFirst();
    }

    protected abstract T mapIterable(IterableSource iterableSource, InferredType elementType, MappingContext context);

}
