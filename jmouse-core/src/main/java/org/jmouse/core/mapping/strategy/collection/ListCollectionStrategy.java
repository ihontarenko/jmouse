package org.jmouse.core.mapping.strategy.collection;

import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.support.AbstractCollectionStrategy;

import java.util.Collection;
import java.util.function.Supplier;

public class ListCollectionStrategy extends AbstractCollectionStrategy {

    @Override
    protected Supplier<Collection<Object>> getCollectionFactory(MappingContext context) {
        return () -> context.config().listFactory().get();
    }

}
