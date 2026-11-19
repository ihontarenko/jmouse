package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;

public record JavaAnnotationInput(BytesCursor bytes, JavaConstantPoolNode pool) {}
