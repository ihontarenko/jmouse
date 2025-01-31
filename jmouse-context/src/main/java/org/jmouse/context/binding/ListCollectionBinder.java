package org.jmouse.context.binding;

import java.util.List;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

public class ListCollectionBinder extends CollectionBinder {

    public ListCollectionBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(List.class).matches(bindable.getType().getRawType());
    }
}
