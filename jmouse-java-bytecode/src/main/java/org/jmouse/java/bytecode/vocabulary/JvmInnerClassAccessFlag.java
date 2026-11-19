package org.jmouse.java.bytecode.vocabulary;

public enum JvmInnerClassAccessFlag implements JvmToken {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    INTERFACE,
    ABSTRACT,
    SYNTHETIC,
    ANNOTATION,
    ENUM;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmInnerClassAccessFlag.class);
    }

    public static JvmInnerClassAccessFlag of(int code) {
        return JvmVocabulary.token(JvmInnerClassAccessFlag.class, code);
    }
}
