package org.jmouse.java.bytecode.vocabulary;

public enum JvmReferenceKind implements JvmToken {
    REF_GET_FIELD,
    REF_GET_STATIC,
    REF_PUT_FIELD,
    REF_PUT_STATIC,
    REF_INVOKE_VIRTUAL,
    REF_INVOKE_STATIC,
    REF_INVOKE_SPECIAL,
    REF_NEW_INVOKE_SPECIAL,
    REF_INVOKE_INTERFACE;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmReferenceKind.class);
    }

    public static JvmReferenceKind of(int code) {
        return JvmVocabulary.token(JvmReferenceKind.class, code);
    }
}
