package org.jmouse.el;

/** A cursor index is not necessarily an offset into its source. */
public enum InputUnit {
    UTF16_CODE_UNIT,
    BYTE,
    TOKEN
}
