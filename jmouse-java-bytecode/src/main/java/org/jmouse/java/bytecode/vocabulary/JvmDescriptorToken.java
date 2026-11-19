package org.jmouse.java.bytecode.vocabulary;

public enum JvmDescriptorToken implements JvmTerminalToken {
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
    VOID;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmDescriptorToken.class);
    }

    public static JvmDescriptorToken of(int code) {
        return JvmVocabulary.token(JvmDescriptorToken.class, code);
    }

    @Override
    public JvmDescriptorToken[] getTokens() {
        return values();
    }
}
