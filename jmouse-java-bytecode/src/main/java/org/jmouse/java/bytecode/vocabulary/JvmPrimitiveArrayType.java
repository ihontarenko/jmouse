package org.jmouse.java.bytecode.vocabulary;

public enum JvmPrimitiveArrayType implements JvmToken {
    T_BOOLEAN,
    T_CHAR,
    T_FLOAT,
    T_DOUBLE,
    T_BYTE,
    T_SHORT,
    T_INT,
    T_LONG;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmPrimitiveArrayType.class);
    }

    public static JvmPrimitiveArrayType of(int code) {
        return JvmVocabulary.token(JvmPrimitiveArrayType.class, code);
    }
}
