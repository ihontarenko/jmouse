package org.jmouse.context.bind;

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
