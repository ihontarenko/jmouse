package org.jmouse.java.bytecode.vocabulary;

public enum JvmClassAccessFlag implements JvmToken {
    PUBLIC,
    FINAL,
    SUPER,
    INTERFACE,
    ABSTRACT,
    SYNTHETIC,
    ANNOTATION,
    ENUM,
    MODULE;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmClassAccessFlag.class);
    }

    public static JvmClassAccessFlag of(int code) {
        return JvmVocabulary.token(JvmClassAccessFlag.class, code);
    }
}
