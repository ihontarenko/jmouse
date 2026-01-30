package org.jmouse.core.mapping.errors;

public enum ErrorAction {
    THROW,   // rethrow
    WARN,    // log + continue (root returns null)
    SILENT   // ignore (root returns null)
}