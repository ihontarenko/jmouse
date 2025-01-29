package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.util.Sorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultBinderFactory implements BinderFactory {

    private final List<ObjectBinder> binders = new ArrayList<>();

    public DefaultBinderFactory() {
        registerBinder(new JavaBeanBinder());
    }

    @Override
    public ObjectBinder getBinder(JavaType type) {
        ObjectBinder binder = null;

        Sorter.sort(this.binders);

        for (ObjectBinder objectBinder : binders) {
            if (objectBinder.supports(type)) {
                binder = objectBinder;
                break;
            }
        }

        if (binder == null) {
            throw new BinderException("No binder found for type " + type);
        }

        return binder;
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
