package org.jmouse.core.mapping.plan.array;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.config.ArrayMaterializationPolicy;
import org.jmouse.core.mapping.plan.support.AbstractIterablePlan;
import org.jmouse.core.mapping.plan.support.IterableSource;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

public final class ArrayPlan extends AbstractIterablePlan<Object> {

    public ArrayPlan(TypedValue<Object> typedValue) {
        super(typedValue);
    }

    @Override
    protected Object mapIterable(IterableSource iterableSource, InferredType elementType, MappingContext context) {
        ArrayMaterializationPolicy policy    = context.config().arrayMaterializationPolicy();
        OptionalInt                knownSize = iterableSource.knownSize();

        if (knownSize.isPresent()) {
            int    size  = knownSize.getAsInt();
            Object array = Array.newInstance(elementType.getClassType(), size);

            if (iterableSource.isIndexed()) {
                for (int index = 0; index < size; index++) {
                    Object adapted = adaptValue(iterableSource.get(index), elementType, context);
                    Array.set(array, index, adapted);
                }
            } else {
                int index = 0;
                for (Iterator<?> iterator = iterableSource.iterator(); iterator.hasNext();) {
                    Object adapted = adaptValue(iterator.next(), elementType, context);
                    Array.set(array, index++, adapted);
                }
            }

            return array;
        }


        List<Object> temporary = new ArrayList<>();
        int          maximum   = context.config().maxCollectionSize();
        int          count     = 0;

        for (Iterator<?> iterator = iterableSource.iterator(); iterator.hasNext();) {
            if (++count > maximum) {
                throw new IllegalStateException("Array size exceeds maxCollectionSize=" + maximum);
            }
            temporary.add(adaptValue(iterator.next(), elementType, context));
        }

        Object array = Array.newInstance(elementType.getClassType(), temporary.size());

        for (int i = 0; i < temporary.size(); i++) {
            Array.set(array, i, temporary.get(i));
        }

        return array;
    }

}
