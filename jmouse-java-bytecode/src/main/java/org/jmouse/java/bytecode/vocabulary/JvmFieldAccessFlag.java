package org.jmouse.java.bytecode.vocabulary;

public enum JvmFieldAccessFlag implements JvmToken {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    VOLATILE,
    TRANSIENT,
    SYNTHETIC,
    ENUM;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmFieldAccessFlag.class);
    }

    public static JvmFieldAccessFlag of(int code) {
        return JvmVocabulary.token(JvmFieldAccessFlag.class, code);
    }
}
