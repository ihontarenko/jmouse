package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;

/** Classfile modified UTF-8 retains UTF-16 code units, including separately encoded surrogates. */
final class JvmModifiedUtf8 {
    private static final int ASCII_LIMIT = 0x80;
    private static final int CONTINUATION_MASK = 0xC0;
    private static final int CONTINUATION_PREFIX = 0x80;
    private static final int CONTINUATION_BITS = 6;
    private static final int CONTINUATION_VALUE = 0x3F;
    private static final int TWO_BYTE_PREFIX = 0xC0;
    private static final int THREE_BYTE_PREFIX = 0xE0;
    private static final int FOUR_BYTE_PREFIX = 0xF0;
    private static final int TWO_BYTE_VALUE = 0x1F;
    private static final int THREE_BYTE_VALUE = 0x0F;
    private static final int THREE_BYTE_MINIMUM = 0x800;

    private JvmModifiedUtf8() {}

    static String read(BytesCursor input) {
        BytesCursor bytes = input.region(input.unsigned16());
        StringBuilder value = new StringBuilder(Math.toIntExact(bytes.remaining()));
        while (bytes.remaining() > 0) {
            int first = bytes.unsigned8();
            if (first > 0 && first < ASCII_LIMIT) {
                value.append((char) first);
            } else if (first >= TWO_BYTE_PREFIX && first < THREE_BYTE_PREFIX) {
                int decoded = (first & TWO_BYTE_VALUE) << CONTINUATION_BITS | continuation(bytes);
                if (decoded != 0 && decoded < ASCII_LIMIT) {
                    throw bytes.failure("Overlong modified UTF-8 sequence");
                }
                value.append((char) decoded);
            } else if (first >= THREE_BYTE_PREFIX && first < FOUR_BYTE_PREFIX) {
                int decoded =
                        (first & THREE_BYTE_VALUE) << (CONTINUATION_BITS * 2)
                                | continuation(bytes) << CONTINUATION_BITS
                                | continuation(bytes);
                if (decoded < THREE_BYTE_MINIMUM) {
                    throw bytes.failure("Overlong modified UTF-8 sequence");
                }
                value.append((char) decoded);
            } else {
                throw bytes.failure("Invalid modified UTF-8 leading byte");
            }
        }
        return value.toString();
    }

    private static int continuation(BytesCursor bytes) {
        int value = bytes.unsigned8();
        if ((value & CONTINUATION_MASK) != CONTINUATION_PREFIX) {
            throw bytes.failure("Invalid modified UTF-8 continuation");
        }
        return value & CONTINUATION_VALUE;
    }
}
