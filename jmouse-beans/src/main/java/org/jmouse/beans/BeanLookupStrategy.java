package org.jmouse.beans;

public enum BeanLookupStrategy {
    /**
     * Delegates both bean instance and definition lookup to parent.
     */
    DELEGATE_TO_PARENT,

    /**
     * Looks up definition from parent if missing, but creates bean locally.
     */
    INHERIT_DEFINITION
}
