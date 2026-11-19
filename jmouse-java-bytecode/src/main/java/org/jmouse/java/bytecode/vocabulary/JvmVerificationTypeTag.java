package org.jmouse.java.bytecode.vocabulary;

public enum JvmVerificationTypeTag implements JvmToken {
    TOP,
    INTEGER,
    FLOAT,
    DOUBLE,
    LONG,
    NULL,
    UNINITIALIZED_THIS,
    OBJECT,
    UNINITIALIZED;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmVerificationTypeTag.class);
    }

    public static JvmVerificationTypeTag of(int code) {
        return JvmVocabulary.token(JvmVerificationTypeTag.class, code);
    }
}
