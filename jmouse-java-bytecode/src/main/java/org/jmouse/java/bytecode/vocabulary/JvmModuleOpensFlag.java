package org.jmouse.java.bytecode.vocabulary;

public enum JvmModuleOpensFlag implements JvmToken {
    SYNTHETIC,
    MANDATED;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmModuleOpensFlag.class);
    }

    public static JvmModuleOpensFlag of(int code) {
        return JvmVocabulary.token(JvmModuleOpensFlag.class, code);
    }
}
