package org.jmouse.java.bytecode.validation;

/** Format revisions that change structural constraints rather than source language syntax. */
public final class JvmValidationVersions {
    public static final int CLASS_LITERALS = 49;
    public static final int METHOD_HANDLES = 51;
    public static final int INTERFACE_STATIC_SPECIAL_CALLS = 52;
    public static final int STATIC_CLASS_INITIALIZER = 51;
    public static final int MODULES = 53;
    public static final int DYNAMIC_CONSTANTS = 55;

    private JvmValidationVersions() {}
}
