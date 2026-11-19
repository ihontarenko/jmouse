package org.jmouse.java.bytecode.vocabulary;

public enum JvmMethodParameterAccessFlag implements JvmToken {
    FINAL,
    SYNTHETIC,
    MANDATED;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmMethodParameterAccessFlag.class);
    }

    public static JvmMethodParameterAccessFlag of(int code) {
        return JvmVocabulary.token(JvmMethodParameterAccessFlag.class, code);
    }
}
