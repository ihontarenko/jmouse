package org.jmouse.core.mapping.strategy.collection;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.support.AbstractCollectionStrategy;

import java.util.Collection;
import java.util.function.Supplier;

public class SetStrategy extends AbstractCollectionStrategy {

    public SetStrategy(TypedValue<Collection<Object>> typedValue) {
        super(typedValue);
    }

    @Override
    protected Supplier<Collection<Object>> getCollectionFactory(MappingContext context) {
        return () -> context.config().setFactory().get();
    }

}
