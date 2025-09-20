package org.jmouse.cglib.core;

import org.objectweb.asm.Opcodes;

final class AsmApi {

    /**
     * Returns the latest stable ASM API value in {@link Opcodes}.
     */
    static int value() {
        return Opcodes.ASM7;
    }

    private AsmApi() {
    }
}
