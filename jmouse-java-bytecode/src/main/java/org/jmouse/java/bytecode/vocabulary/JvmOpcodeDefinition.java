package org.jmouse.java.bytecode.vocabulary;

public record JvmOpcodeDefinition(
        int code,
        String mnemonic,
        JvmOpcodeClassification classification,
        JvmOperandEncoding operands,
        String family,
        int localSlot,
        int localWidth,
        int minimumMajor,
        int maximumMajor) {}
