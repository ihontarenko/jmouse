package org.jmouse.context.binding;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

public class ListBinder extends CollectionBinder {

    public ListBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(List.class).matches(bindable.getType().getRawType());
    }

    @Override
    protected <T> Supplier<? extends Collection<T>> getCollectionSupplier() {
        return LinkedList::new;
    }
}
