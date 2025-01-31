package org.jmouse.context.binding;

public enum BindingStrategy {

    /**
     * Bind only values (not recursively)
     */
    SHALLOW,

    /**
     * Bind all nested objects recursively
     */
    DEEP
}
