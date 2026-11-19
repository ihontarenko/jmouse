package org.jmouse.java.bytecode.vocabulary;

public enum JvmTypePathKind implements JvmToken {
    ARRAY_ELEMENT,
    NESTED_TYPE,
    WILDCARD_BOUND,
    TYPE_ARGUMENT;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmTypePathKind.class);
    }

    public static JvmTypePathKind of(int code) {
        return JvmVocabulary.token(JvmTypePathKind.class, code);
    }
}
