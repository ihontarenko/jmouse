package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;

public record JavaStackMapInput(BytesCursor bytes, JavaConstantPoolNode pool) {}
