package org.jmouse.web.binding;

import org.jmouse.core.access.TypedValue;

import java.util.Map;

public interface ParametersDataBinder {

    /**
     * 🎯 Logical object name used by plugins/diagnostics.
     */
    String getObjectName();

    /**
     * 🔎 Raw input parameters source.
     */
    Map<String, String[]> getParameters();

    /**
     * 🧩 Bind parameters into a new target described by {@link TypedValue}.
     */
    <T> T bind(TypedValue<T> target);

    /**
     * 🧩 Bind parameters into an existing target instance.
     */
    default <T> T bindInto(T instance) {
        return bind(TypedValue.ofInstance(instance));
    }
}
