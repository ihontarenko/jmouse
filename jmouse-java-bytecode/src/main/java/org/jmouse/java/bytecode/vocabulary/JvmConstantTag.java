package org.jmouse.java.bytecode.vocabulary;

public enum JvmConstantTag implements JvmToken, JvmConstantKind {
    UTF8,
    INTEGER,
    FLOAT,
    LONG,
    DOUBLE,
    CLASS,
    STRING,
    FIELDREF,
    METHODREF,
    INTERFACE_METHODREF,
    NAME_AND_TYPE,
    METHOD_HANDLE,
    METHOD_TYPE,
    DYNAMIC,
    INVOKE_DYNAMIC,
    MODULE,
    PACKAGE;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmConstantTag.class);
    }

    public static JvmConstantTag of(int code) {
        return JvmVocabulary.token(JvmConstantTag.class, code);
    }

    @Override
    public int slots() {
        return this == LONG || this == DOUBLE
                ? JvmFormatLimits.WIDE_CONSTANT_SLOTS
                : JvmFormatLimits.SINGLE_CONSTANT_SLOT;
    }

    @Override
    public int code() {
        return JvmToken.super.code();
    }
}
