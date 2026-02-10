package org.jmouse.core.mapping.strategy.array;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.errors.ErrorCodes;
import org.jmouse.core.mapping.strategy.support.AbstractIterableStrategy;
import org.jmouse.core.mapping.strategy.support.IterableSource;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.util.Arrays;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

public final class ArrayStrategy extends AbstractIterableStrategy<Object> {

    @Override
    protected Object mapIterable(
            IterableSource iterableSource,
            TypedValue<?> typedValue,
            InferredType elementType,
            MappingContext context
    ) {
        OptionalInt knownSize = iterableSource.knownSize();

        if (knownSize.isPresent()) {
            int    size   = knownSize.getAsInt();
            Object array  = getTargetArray(typedValue, elementType, size);
            int    offset = Array.getLength(array) - size;

            size += offset;

            if (iterableSource.isIndexed()) {
                for (int index = offset; index < size; index++) {
                    Object adapted = adaptValue(iterableSource.get(index - offset), elementType, context);
                    Array.set(array, index, adapted);
                }
            } else {
                int index = offset;
                for (Iterator<?> iterator = iterableSource.iterator(); iterator.hasNext();) {
                    Object adapted = adaptValue(iterator.next(), elementType, context);
                    Array.set(array, index++, adapted);
                }
            }

            return array;
        }


        List<Object> temporary = new ArrayList<>();
        int          maximum   = context.config().maxCollectionSize();
        int          counter   = 0;

        for (Iterator<?> iterator = iterableSource.iterator(); iterator.hasNext();) {
            if (++counter > maximum) {
                throw toMappingException(
                        ErrorCodes.ARRAY_SIZE_EXCEEDS,
                        "Array size exceeds maxCollectionSize=" + maximum,
                        null
                );
            }
            MappingContext mappingContext = context.appendPath("[" + (counter - 1) + "]");
            temporary.add(adaptValue(iterator.next(), elementType, mappingContext));
        }

        Object array = getTargetArray(typedValue, elementType, temporary.size());

        for (int i = 0; i < temporary.size(); i++) {
            Array.set(array, i, temporary.get(i));
        }

        return array;
    }

    private Object getTargetArray(TypedValue<?> typedValue, InferredType elementType, int size) {
        Object array = typedValue.getValue().get();

        if (array == null) {
            array = Array.newInstance(elementType.getClassType(), size);
        } else {
            array = Arrays.expand(array, size + Array.getLength(array));
        }

        return array;
    }

}
