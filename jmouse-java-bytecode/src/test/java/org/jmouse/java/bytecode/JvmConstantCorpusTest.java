package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.constant.*;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class JvmConstantCorpusTest {
    @Test
    void dynamicConstantDescriptorsChooseTheCorrectLdcComputationalCategory() {
        for (String descriptor : new String[] {"I", "J"}) {
            ClassWriter writer = owner();
            MethodVisitor method =
                    writer.visitMethod(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "value", "()V", null, null);
            method.visitCode();
            ConstantDynamic constant =
                    new ConstantDynamic(
                            "value",
                            descriptor,
                            new Handle(
                                    Opcodes.H_INVOKESTATIC,
                                    "sample/Bootstrap",
                                    "constant",
                                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",
                                    false));
            method.visitLdcInsn(constant);
            method.visitInsn(descriptor.equals("J") ? Opcodes.POP2 : Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(2, 0);
            method.visitEnd();
            writer.visitEnd();
            JavaClassFileNode root =
                    new JavaBytecodeParser()
                            .parse("dynamic.class", writer.toByteArray())
                            .requireComplete();
            assertEquals(
                    1,
                    root.pool().entries().stream()
                            .filter(JavaDynamicConstantNode.class::isInstance)
                            .count());
            JavaCodeAttributeNode code =
                    root.methods().getFirst().attributes().stream()
                            .filter(JavaCodeAttributeNode.class::isInstance)
                            .map(JavaCodeAttributeNode.class::cast)
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    descriptor.equals("J") ? JvmOpcode.LDC2_W : JvmOpcode.LDC,
                    code.instructions().getFirst().opcode());
        }
    }

    @Test
    void floatingPointPayloadsAndWideConstantPoolHolesArePreserved() {
        long doubleBits = 0x7ff8000000000042L;
        int floatBits = 0x7fc00042;
        ClassWriter writer = owner();
        writer.visitField(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        "doubleValue",
                        "D",
                        null,
                        Double.longBitsToDouble(doubleBits))
                .visitEnd();
        writer.visitField(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        "floatValue",
                        "F",
                        null,
                        Float.intBitsToFloat(floatBits))
                .visitEnd();
        writer.visitField(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        "longValue",
                        "J",
                        null,
                        Long.MIN_VALUE)
                .visitEnd();
        writer.visitEnd();
        JavaClassFileNode root =
                new JavaBytecodeParser()
                        .parse("bits.class", writer.toByteArray())
                        .requireComplete();
        JavaDoubleConstantNode doubleValue =
                root.pool().entries().stream()
                        .filter(JavaDoubleConstantNode.class::isInstance)
                        .map(JavaDoubleConstantNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        JavaFloatConstantNode floatValue =
                root.pool().entries().stream()
                        .filter(JavaFloatConstantNode.class::isInstance)
                        .map(JavaFloatConstantNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        JavaLongConstantNode longValue =
                root.pool().entries().stream()
                        .filter(JavaLongConstantNode.class::isInstance)
                        .map(JavaLongConstantNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        assertEquals(doubleBits, doubleValue.bits());
        assertEquals(floatBits, floatValue.bits());
        assertEquals(Long.MIN_VALUE, longValue.value());
        assertThrows(
                IllegalArgumentException.class, () -> root.pool().entry(doubleValue.index() + 1));
        assertThrows(
                IllegalArgumentException.class, () -> root.pool().entry(longValue.index() + 1));
    }

    @Test
    void modifiedUtf8KeepsNullAndSurrogateCodeUnitsAndRejectsRawZeroBytes() {
        String text = "zero\0-surrogate\uD800-pair\uD83D\uDE00";
        ClassWriter writer = owner();
        writer.visitField(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        "text",
                        "Ljava/lang/String;",
                        null,
                        text)
                .visitEnd();
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        JavaClassFileNode root =
                new JavaBytecodeParser().parse("utf8.class", bytes).requireComplete();
        JavaStringConstantNode string =
                root.pool().entries().stream()
                        .filter(JavaStringConstantNode.class::isInstance)
                        .map(JavaStringConstantNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        JavaUtf8ConstantNode encoded =
                root.pool().require(string.stringIndex(), JavaUtf8ConstantNode.class);
        assertEquals(text, encoded.value());
        bytes[Math.toIntExact(encoded.span().start()) + 3] = 0;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("invalid-utf8.class", bytes));
    }

    private static ClassWriter owner() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "sample/Constants",
                null,
                "java/lang/Object",
                null);
        return writer;
    }
}
