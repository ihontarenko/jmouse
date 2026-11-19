package org.jmouse.java.bytecode.vocabulary;

public enum JvmModuleRequiresFlag implements JvmToken {
    TRANSITIVE,
    STATIC_PHASE,
    SYNTHETIC,
    MANDATED;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmModuleRequiresFlag.class);
    }

    public static JvmModuleRequiresFlag of(int code) {
        return JvmVocabulary.token(JvmModuleRequiresFlag.class, code);
    }
}
