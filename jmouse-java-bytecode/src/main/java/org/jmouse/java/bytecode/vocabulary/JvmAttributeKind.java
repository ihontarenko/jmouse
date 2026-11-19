package org.jmouse.java.bytecode.vocabulary;

import java.util.Arrays;
import java.util.Optional;

public enum JvmAttributeKind {
    CONSTANT_VALUE,
    CODE,
    STACK_MAP_TABLE,
    EXCEPTIONS,
    INNER_CLASSES,
    ENCLOSING_METHOD,
    SYNTHETIC,
    SIGNATURE,
    SOURCE_FILE,
    SOURCE_DEBUG_EXTENSION,
    LINE_NUMBER_TABLE,
    LOCAL_VARIABLE_TABLE,
    LOCAL_VARIABLE_TYPE_TABLE,
    DEPRECATED,
    RUNTIME_VISIBLE_ANNOTATIONS,
    RUNTIME_INVISIBLE_ANNOTATIONS,
    RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
    RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
    RUNTIME_VISIBLE_TYPE_ANNOTATIONS,
    RUNTIME_INVISIBLE_TYPE_ANNOTATIONS,
    ANNOTATION_DEFAULT,
    BOOTSTRAP_METHODS,
    METHOD_PARAMETERS,
    MODULE,
    MODULE_PACKAGES,
    MODULE_MAIN_CLASS,
    NEST_HOST,
    NEST_MEMBERS,
    RECORD,
    PERMITTED_SUBCLASSES;

    public String encodedName() {
        return JvmVocabulary.attribute(this).name();
    }

    public int minimumMajor() {
        return JvmVocabulary.attribute(this).major();
    }

    public static Optional<JvmAttributeKind> named(String name) {
        return Arrays.stream(values()).filter(kind -> kind.encodedName().equals(name)).findFirst();
    }
}
