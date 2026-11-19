package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;

public record JavaInstructionInput(BytesCursor bytes, long codeStart) {}
