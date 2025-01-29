package org.jmouse.context.binding;

public interface BindCallback {

    default void onSuccess(Bindable<?> bindable) {
        // no-op
    }

}
