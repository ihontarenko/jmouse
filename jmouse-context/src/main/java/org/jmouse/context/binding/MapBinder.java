package org.jmouse.context.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

public class MapBinder extends CollectionBinder {

    public MapBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(Map.class).matches(bindable.getType().getRawType());
    }

    @Override
    protected <T> Supplier<? extends Collection<T>> getCollection() {
        return ArrayList::new;
    }
}
