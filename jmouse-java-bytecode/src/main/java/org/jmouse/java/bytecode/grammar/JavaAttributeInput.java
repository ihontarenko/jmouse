package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeLanguage;
import org.jmouse.java.bytecode.JavaBytecodeMode;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeContext;
import org.jmouse.java.model.version.ClassFileVersion;

public record JavaAttributeInput(
        BytesCursor bytes,
        BytecodeSpan span,
        JavaConstantPoolNode pool,
        int nameIndex,
        JvmAttributeContext context,
        ClassFileVersion version,
        JavaBytecodeMode mode,
        JavaBytecodeLanguage language) {}
