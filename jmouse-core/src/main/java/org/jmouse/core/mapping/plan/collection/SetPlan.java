package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.plan.support.AbstractCollectionPlan;

import java.util.Collection;
import java.util.function.Supplier;

public class SetPlan extends AbstractCollectionPlan {

    public SetPlan(TypedValue<Collection<Object>> typedValue) {
        super(typedValue);
    }

    @Override
    protected Supplier<Collection<Object>> getCollectionFactory(MappingContext context) {
        return () -> context.config().setFactory().get();
    }

}
