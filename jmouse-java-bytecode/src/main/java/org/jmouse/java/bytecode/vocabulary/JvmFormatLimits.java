package org.jmouse.java.bytecode.vocabulary;

/** Fixed format invariants; resource budgets and admission policies are configured separately. */
public final class JvmFormatLimits {
    public static final long MAGIC = 0xCAFEBABEL;
    public static final int SINGLE_CONSTANT_SLOT = 1;
    public static final int WIDE_CONSTANT_SLOTS = 2;
    public static final int MAXIMUM_CODE_BYTES = 65535;
    public static final int MAXIMUM_UTF8_BYTES = 65535;
    public static final int SWITCH_ALIGNMENT = Integer.BYTES;
    public static final int NO_INDEX = 0;
    public static final int SUPERCLASS_TYPE_ANNOTATION = 0xFFFF;
    public static final int ATTRIBUTE_HEADER_BYTES = Short.BYTES + Integer.BYTES;

    private JvmFormatLimits() {}
}
