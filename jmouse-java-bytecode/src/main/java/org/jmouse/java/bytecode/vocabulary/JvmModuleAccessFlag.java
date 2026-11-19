package org.jmouse.java.bytecode.vocabulary;

public enum JvmModuleAccessFlag implements JvmToken {
    OPEN,
    SYNTHETIC,
    MANDATED;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmModuleAccessFlag.class);
    }

    public static JvmModuleAccessFlag of(int code) {
        return JvmVocabulary.token(JvmModuleAccessFlag.class, code);
    }
}
