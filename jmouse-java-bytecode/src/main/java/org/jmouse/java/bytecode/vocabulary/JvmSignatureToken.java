package org.jmouse.java.bytecode.vocabulary;

public enum JvmSignatureToken implements JvmTerminalToken {
    BYTE,
    CHAR,
    DOUBLE,
    FLOAT,
    INT,
    LONG,
    SHORT,
    BOOLEAN,
    CLASS,
    SEMICOLON,
    ARRAY,
    PARAMETERS_OPEN,
    PARAMETERS_CLOSE,
    VOID,
    TYPE_VARIABLE,
    BOUND,
    EXTENDS,
    SUPER,
    UNBOUNDED,
    ARGUMENTS_OPEN,
    ARGUMENTS_CLOSE,
    INNER_TYPE,
    THROWS,
    PACKAGE_SEPARATOR;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmSignatureToken.class);
    }

    public static JvmSignatureToken of(int code) {
        return JvmVocabulary.token(JvmSignatureToken.class, code);
    }

    @Override
    public JvmSignatureToken[] getTokens() {
        return values();
    }
}
