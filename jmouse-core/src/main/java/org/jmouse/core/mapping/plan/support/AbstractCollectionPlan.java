package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class AbstractCollectionPlan extends AbstractIterablePlan<Collection<Object>> {

    protected AbstractCollectionPlan(InferredType targetType) {
        super(targetType);
    }

    @Override
    protected final Collection<Object> mapIterable(IterableSource iterableSource, InferredType elementType, MappingContext context) {
        Collection<Object> target  = getCollectionFactory(context).get();
        int                maximum = context.config().maxCollectionSize();
        int                count   = 0;

        for (var iterator = iterableSource.iterator(); iterator.hasNext(); ) {
            if (++count > maximum) {
                throw new IllegalStateException("Collection size exceeds maxCollectionSize=" + maximum);
            }
            target.add(adaptValue(iterator.next(), elementType, context));
        }

        return target;
    }

    protected abstract Supplier<Collection<Object>> getCollectionFactory(MappingContext context);
}
