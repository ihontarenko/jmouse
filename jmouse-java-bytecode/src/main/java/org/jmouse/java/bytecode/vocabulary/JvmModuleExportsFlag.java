package org.jmouse.java.bytecode.vocabulary;

public enum JvmModuleExportsFlag implements JvmToken {
    SYNTHETIC,
    MANDATED;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmModuleExportsFlag.class);
    }

    public static JvmModuleExportsFlag of(int code) {
        return JvmVocabulary.token(JvmModuleExportsFlag.class, code);
    }
}
