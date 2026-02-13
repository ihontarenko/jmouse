package org.jmouse.web.binding;

public interface BindingProcessor {

    default void onStart(BindingStart start, BindingSession session) {
    }

    default Object onValue(BindingValue value, BindingSession session) {
        return value.current();
    }

    default void onFinish(BindingFinish finish, BindingSession session) {
    }

    default void onError(BindingError error, BindingSession session) {
    }
}
