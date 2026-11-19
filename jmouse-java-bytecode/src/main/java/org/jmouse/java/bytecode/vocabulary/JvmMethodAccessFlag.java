package org.jmouse.java.bytecode.vocabulary;

public enum JvmMethodAccessFlag implements JvmToken {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    SYNCHRONIZED,
    BRIDGE,
    VARARGS,
    NATIVE,
    ABSTRACT,
    STRICT,
    SYNTHETIC;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmMethodAccessFlag.class);
    }

    public static JvmMethodAccessFlag of(int code) {
        return JvmVocabulary.token(JvmMethodAccessFlag.class, code);
    }
}
