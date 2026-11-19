package org.jmouse.java.bytecode.vocabulary;

public enum JvmAnnotationValueTag implements JvmToken {
    BYTE,
    CHAR,
    DOUBLE,
    FLOAT,
    INT,
    LONG,
    SHORT,
    BOOLEAN,
    STRING,
    ENUM,
    CLASS,
    ANNOTATION,
    ARRAY;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmAnnotationValueTag.class);
    }

    public static JvmAnnotationValueTag of(int code) {
        return JvmVocabulary.token(JvmAnnotationValueTag.class, code);
    }
}
