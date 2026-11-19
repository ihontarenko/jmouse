package org.jmouse.java.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Independent binary encoding in tests; production decoding has no ASM dependency. */
final class JvmOpcodeFixtures {
    private JvmOpcodeFixtures() {
    }

    static byte[] instruction(int opcode) {
        ClassWriter owner = owner(opcode == Opcodes.JSR || opcode == Opcodes.RET || opcode == 201
                ? Opcodes.V1_6 : Opcodes.V21);
        if (opcode == 19) {
            for (int index = 0; index < 300; index++) {
                owner.newConst("padding-" + index);
            }
        }
        String descriptor = switch (opcode) {
            case Opcodes.IRETURN -> "()I";
            case Opcodes.LRETURN -> "()J";
            case Opcodes.FRETURN -> "()F";
            case Opcodes.DRETURN -> "()D";
            case Opcodes.ARETURN -> "()Ljava/lang/Object;";
            default -> "()V";
        };
        MethodVisitor method = owner.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "decode", descriptor, null, null);
        method.visitCode();
        Label start = new Label();
        method.visitLabel(start);
        switch (opcode) {
            case Opcodes.BIPUSH, Opcodes.SIPUSH -> method.visitIntInsn(opcode, -7);
            case Opcodes.LDC, 19 -> method.visitLdcInsn("literal");
            case 20 -> method.visitLdcInsn(42L);
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD,
                    Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE,
                    Opcodes.RET -> method.visitVarInsn(opcode, 4);
            case Opcodes.IINC -> method.visitIincInsn(4, -2);
            case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
                    Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
                    Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE,
                    Opcodes.GOTO, Opcodes.JSR, Opcodes.IFNULL, Opcodes.IFNONNULL -> method.visitJumpInsn(opcode, start);
            case Opcodes.TABLESWITCH -> method.visitTableSwitchInsn(1, 2, start, start, start);
            case Opcodes.LOOKUPSWITCH -> method.visitLookupSwitchInsn(start, new int[] {-2, 5}, new Label[] {start, start});
            case Opcodes.GETSTATIC, Opcodes.PUTSTATIC, Opcodes.GETFIELD, Opcodes.PUTFIELD ->
                    method.visitFieldInsn(opcode, "java/lang/System", "out", "Ljava/io/PrintStream;");
            case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESTATIC, Opcodes.INVOKESPECIAL ->
                    method.visitMethodInsn(opcode, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
            case Opcodes.INVOKEINTERFACE ->
                    method.visitMethodInsn(opcode, "java/util/List", "size", "()I", true);
            case Opcodes.INVOKEDYNAMIC -> method.visitInvokeDynamicInsn("call", "()V",
                    new Handle(Opcodes.H_INVOKESTATIC, "sample/Bootstrap", "bootstrap",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false));
            case Opcodes.NEW, Opcodes.ANEWARRAY, Opcodes.CHECKCAST, Opcodes.INSTANCEOF ->
                    method.visitTypeInsn(opcode, "java/lang/Object");
            case Opcodes.NEWARRAY -> method.visitIntInsn(opcode, Opcodes.T_INT);
            case Opcodes.MULTIANEWARRAY -> method.visitMultiANewArrayInsn("[[I", 2);
            case 196 -> method.visitVarInsn(Opcodes.ILOAD, 300);
            case 200, 201 -> {
                Label far = new Label();
                method.visitJumpInsn(opcode == 200 ? Opcodes.GOTO : Opcodes.JSR, far);
                for (int index = 0; index < 40000; index++) {
                    method.visitInsn(Opcodes.NOP);
                }
                method.visitLabel(far);
            }
            default -> method.visitInsn(opcode);
        }
        if (opcode < Opcodes.IRETURN || opcode > Opcodes.RETURN) {
            method.visitInsn(Opcodes.RETURN);
        }
        method.visitMaxs(512, 512);
        method.visitEnd();
        owner.visitEnd();
        return owner.toByteArray();
    }

    static byte[] wide(int opcode) {
        ClassWriter owner = owner(opcode == Opcodes.RET ? Opcodes.V1_6 : Opcodes.V21);
        MethodVisitor method = owner.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "decode", "()V", null, null);
        method.visitCode();
        if (opcode == Opcodes.IINC) {
            method.visitIincInsn(300, -300);
        } else {
            method.visitVarInsn(opcode, 300);
        }
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(512, 512);
        method.visitEnd();
        owner.visitEnd();
        return owner.toByteArray();
    }

    private static ClassWriter owner(int version) {
        ClassWriter owner = new ClassWriter(0);
        owner.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "sample/OpcodeFixture", null, "java/lang/Object", null);
        return owner;
    }
}

