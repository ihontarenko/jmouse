package org.jmouse.java.bytecode.vocabulary;

public enum JvmModuleNameToken implements JvmToken {
    ESCAPE,
    COLON,
    AT_SIGN,
    CONTROL;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmModuleNameToken.class);
    }
}

