package org.jmouse.mapper.strategy.collection;

import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.strategy.support.AbstractCollectionStrategy;

import java.util.Collection;
import java.util.function.Supplier;

public class SetCollectionStrategy extends AbstractCollectionStrategy {

    @Override
    protected Supplier<Collection<Object>> getCollectionFactory(MappingContext context) {
        return () -> context.config().setFactory().get();
    }

}
