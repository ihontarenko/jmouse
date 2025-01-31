package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

import java.util.Collection;

public interface BinderFactory {

    ObjectBinder getBinderFor(Bindable<?> bindable);

    void registerBinder(ObjectBinder binder);

    Collection<ObjectBinder> getBinders();

    void unregisterBinder(ObjectBinder binder);

    void clearBinders();

}
