package org.jmouse.java.bytecode.signature;

/** Grammar identities and source labels, independent from individual parser implementations. */
public final class JvmTextNames {
    public static final String SOURCE = "JVM descriptor/signature";
    public static final String TYPE_VARIABLE_SIGNATURE = "java.jvm-text.type-variable-signature";
    public static final String ARRAY_SIGNATURE = "java.jvm-text.array-signature";
    public static final String TYPE_ARGUMENT = "java.jvm-text.type-argument";
    public static final String CLASS_TYPE_SEGMENT = "java.jvm-text.class-type-segment";
    public static final String CLASS_TYPE_SIGNATURE = "java.jvm-text.class-type-signature";
    public static final String TYPE_PARAMETER = "java.jvm-text.type-parameter";
    public static final String CLASS_SIGNATURE = "java.jvm-text.class-signature";
    public static final String THROWS_SIGNATURE = "java.jvm-text.throws-signature";
    public static final String METHOD_SIGNATURE = "java.jvm-text.method-signature";
    public static final String JAVA_TYPE_SIGNATURE = "java.jvm-text.java-type-signature";
    public static final String REFERENCE_TYPE_SIGNATURE = "java.jvm-text.reference-type-signature";
    public static final String BOUND = "bound";
    public static final String ARGUMENTS = "arguments";
    public static final String SEGMENTS = "segments";
    public static final String CLASS_BOUND = "class-bound";
    public static final String INTERFACE_BOUNDS = "interface-bounds";
    public static final String TYPE_PARAMETERS = "type-parameters";
    public static final String SUPERCLASS = "superclass";
    public static final String INTERFACES = "interfaces";
    public static final String TYPE = "type";
    public static final String THROWN_TYPES = "thrown-types";
    public static final String SCALAR_DESCRIPTOR = "java.jvm-text.scalar-descriptor";
    public static final String CLASS_DESCRIPTOR = "java.jvm-text.class-descriptor";
    public static final String ARRAY_DESCRIPTOR = "java.jvm-text.array-descriptor";
    public static final String NODE_NAMESPACE = "java.jvm-text.";
    public static final String FIELD_DESCRIPTOR = "java.jvm-text.field-descriptor";
    public static final String RETURN_DESCRIPTOR = "java.jvm-text.return-descriptor";
    public static final String METHOD_DESCRIPTOR = "java.jvm-text.method-descriptor";
    public static final String ELEMENT = "element";
    public static final String PARAMETERS = "parameters";
    public static final String RESULT = "result";
    public static final int TERMINAL_TOKEN_BASE = 50000;
    public static final int CHARACTER_TOKEN = 51000;
    public static final int END_TOKEN = 51001;

    private JvmTextNames() {}
}
