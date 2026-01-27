package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.plan.support.AbstractCollectionPlan;
import org.jmouse.core.reflection.InferredType;

import java.util.Collection;
import java.util.function.Supplier;

public class ListPlan extends AbstractCollectionPlan {

    public ListPlan(InferredType targetType) {
        super(targetType);
    }

    @Override
    protected Supplier<Collection<Object>> getCollectionFactory(MappingContext context) {
        return () -> context.config().listFactory().get();
    }

}
