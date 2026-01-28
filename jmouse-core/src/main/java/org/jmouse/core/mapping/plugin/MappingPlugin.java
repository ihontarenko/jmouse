package org.jmouse.core.mapping.plugin;

public interface MappingPlugin {

    default void onStart(MappingCall call) {
    }

    /**
     * Called when a raw value for some target “slot” is produced (binding/fallback).
     */
    default Object onValue(MappingValue value) {
        return value.current();
    }

    /**
     * Called after the target object is materialized.
     */
    default void onFinish(MappingResult result) {
    }

    /**
     * Optional error hook.
     */
    default void onError(MappingFailure failure) {
    }
}
