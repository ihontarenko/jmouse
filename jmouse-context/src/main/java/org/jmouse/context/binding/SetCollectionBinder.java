package org.jmouse.context.binding;

import java.util.Set;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

public class SetCollectionBinder extends CollectionBinder {

    public SetCollectionBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(Set.class).matches(bindable.getType().getRawType());
    }

}
