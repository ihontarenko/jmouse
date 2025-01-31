package org.jmouse.context.binding;

import java.util.Map;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

public class MapCollectionBinder extends CollectionBinder {

    public MapCollectionBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(Map.class).matches(bindable.getType().getRawType());
    }
}
