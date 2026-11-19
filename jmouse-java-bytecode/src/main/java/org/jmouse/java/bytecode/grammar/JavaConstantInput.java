package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;

/** The tag byte has been consumed; its exact source start and pool index remain explicit. */
public record JavaConstantInput(BytesCursor bytes, long start, int index, int tag) {}
