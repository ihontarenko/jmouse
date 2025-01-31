package org.jmouse.context.binding;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

public class SetBinder extends CollectionBinder {

    public SetBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(Set.class).matches(bindable.getType().getRawType());
    }

    @Override
    protected <T> Supplier<? extends Collection<T>> getCollection() {
        return HashSet::new;
    }
}
