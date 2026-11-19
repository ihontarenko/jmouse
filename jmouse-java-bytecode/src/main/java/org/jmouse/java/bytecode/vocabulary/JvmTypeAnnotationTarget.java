package org.jmouse.java.bytecode.vocabulary;

public enum JvmTypeAnnotationTarget implements JvmToken {
    CLASS_TYPE_PARAMETER,
    METHOD_TYPE_PARAMETER,
    SUPERTYPE,
    CLASS_TYPE_PARAMETER_BOUND,
    METHOD_TYPE_PARAMETER_BOUND,
    FIELD,
    METHOD_RETURN,
    METHOD_RECEIVER,
    METHOD_FORMAL_PARAMETER,
    THROWS,
    LOCAL_VARIABLE,
    RESOURCE_VARIABLE,
    EXCEPTION_PARAMETER,
    INSTANCEOF,
    NEW,
    CONSTRUCTOR_REFERENCE,
    METHOD_REFERENCE,
    CAST,
    CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
    METHOD_INVOCATION_TYPE_ARGUMENT,
    CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
    METHOD_REFERENCE_TYPE_ARGUMENT;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmTypeAnnotationTarget.class);
    }

    public static JvmTypeAnnotationTarget of(int code) {
        return JvmVocabulary.token(JvmTypeAnnotationTarget.class, code);
    }
}
