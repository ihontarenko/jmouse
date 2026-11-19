package org.jmouse.java.bytecode.vocabulary;

public enum JvmOperandEncoding {
    NONE,
    SIGNED_BYTE,
    SIGNED_SHORT,
    CONSTANT_BYTE,
    CONSTANT_SHORT,
    LOCAL,
    INCREMENT,
    BRANCH_SHORT,
    TABLE_SWITCH,
    LOOKUP_SWITCH,
    INTERFACE_CALL,
    DYNAMIC_CALL,
    PRIMITIVE_ARRAY,
    WIDE,
    MULTI_ARRAY,
    BRANCH_INT,
    INVALID
}
