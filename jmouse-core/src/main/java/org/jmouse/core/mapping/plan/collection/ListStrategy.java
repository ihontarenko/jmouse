package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.plan.support.AbstractCollectionStrategy;

import java.util.Collection;
import java.util.function.Supplier;

public class ListStrategy extends AbstractCollectionStrategy {

    public ListStrategy(TypedValue<Collection<Object>> typedValue) {
        super(typedValue);
    }

    @Override
    protected Supplier<Collection<Object>> getCollectionFactory(MappingContext context) {
        return () -> context.config().listFactory().get();
    }

}
