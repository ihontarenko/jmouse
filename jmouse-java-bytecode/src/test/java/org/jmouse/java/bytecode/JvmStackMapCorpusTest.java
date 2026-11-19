package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaVerificationTypeNode;
import org.jmouse.java.bytecode.vocabulary.JvmStackMapFrameKind;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.EnumSet;
import java.util.Set;

class JvmStackMapCorpusTest {
    /**
     * Tests the complete syntax vocabulary without claiming operand-stack type-flow verification.
     */
    @Test
    void everyStandardFrameFormAndVerificationTagHasAnOwnTraversableModel() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "sample/Frames",
                null,
                "java/lang/Object",
                null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        method.visitCode();
        Label allocation = new Label();
        method.visitLabel(allocation);
        method.visitTypeInsn(Opcodes.NEW, "java/lang/Object");
        Object[] locals = {
            Opcodes.TOP,
            Opcodes.INTEGER,
            Opcodes.FLOAT,
            Opcodes.DOUBLE,
            Opcodes.LONG,
            Opcodes.NULL,
            Opcodes.UNINITIALIZED_THIS,
            "java/lang/Object",
            allocation
        };
        method.visitFrame(Opcodes.F_FULL, locals.length, locals, 0, new Object[0]);
        method.visitInsn(Opcodes.NOP);
        method.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        method.visitInsn(Opcodes.NOP);
        method.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
        method.visitInsn(Opcodes.NOP);
        method.visitFrame(Opcodes.F_APPEND, 1, new Object[] {Opcodes.INTEGER}, 0, null);
        method.visitInsn(Opcodes.NOP);
        method.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        for (int index = 0; index < 70; index++) {
            method.visitInsn(Opcodes.NOP);
        }
        method.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        for (int index = 0; index < 70; index++) {
            method.visitInsn(Opcodes.NOP);
        }
        method.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.LONG});
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(32, 32);
        method.visitEnd();
        writer.visitEnd();
        JavaClassFileNode root =
                new JavaBytecodeParser()
                        .parse("frames.class", writer.toByteArray())
                        .requireComplete();
        Set<JvmStackMapFrameKind> frames = EnumSet.noneOf(JvmStackMapFrameKind.class);
        Set<JvmVerificationTypeTag> types = EnumSet.noneOf(JvmVerificationTypeTag.class);
        root.accept(
                new Visitor() {
                    @Override
                    public void visit(Node node) {
                        if (node instanceof JavaStackMapFrameNode frame) {
                            frames.add(frame.kind());
                        } else if (node instanceof JavaVerificationTypeNode type) {
                            types.add(type.tag());
                        }
                    }
                });
        Set<JvmStackMapFrameKind> expected = EnumSet.allOf(JvmStackMapFrameKind.class);
        expected.remove(JvmStackMapFrameKind.RESERVED);
        assertEquals(expected, frames);
        assertEquals(EnumSet.allOf(JvmVerificationTypeTag.class), types);
    }
}
