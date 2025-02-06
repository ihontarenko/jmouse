package org.jmouse.context.bind;

import org.jmouse.util.Sorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultBinderFactory implements BinderFactory {

    private final List<ObjectBinder> binders = new ArrayList<>();

    @Override
    public ObjectBinder getBinderFor(Bindable<?> bindable) {
        Sorter.sort(this.binders);

        for (ObjectBinder binder : binders) {
            if (binder.supports(bindable)) {
                return binder;
            }
        }

        throw new BindException("No binder found for bindable type '%s'.".formatted(bindable.getType()));
    }

    @Override
    public void registerBinder(ObjectBinder binder) {
        this.binders.add(binder);
    }

    @Override
    public Collection<ObjectBinder> getBinders() {
        return this.binders;
    }

    @Override
    public void unregisterBinder(ObjectBinder binder) {
        this.binders.remove(binder);
    }

    @Override
    public void clearBinders() {
        this.binders.clear();
    }
}
