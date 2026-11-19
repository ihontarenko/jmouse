package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.constant.*;
import org.jmouse.java.bytecode.node.instruction.JavaConstantInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaIncrementInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInvokeDynamicInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInvokeInterfaceInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaLocalInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaLookupSwitchInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaMultiArrayInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaNoOperandInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaTableSwitchInstructionNode;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;
import org.jmouse.java.bytecode.vocabulary.JvmOpcodeDefinition;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;
import org.jmouse.java.bytecode.vocabulary.JvmVocabulary;
import org.jmouse.java.model.descriptor.JavaArrayDescriptor;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaVoidType;

public final class JavaInstructionStructureValidation {
    private JavaInstructionStructureValidation() {}

    public static JvmValidationOutcome noOperand(
            JavaNoOperandInstructionNode node, JvmValidationContext context) {
        local(node, context);
        switch (node.opcode()) {
            case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN -> {
                JavaMethodNode method = JvmValidationContext.enclosing(node, JavaMethodNode.class);
                JavaTypeDescriptor result = context.method(method.descriptor()).result();
                boolean compatible =
                        switch (node.opcode()) {
                            case IRETURN ->
                                    result == JavaPrimitiveType.INT
                                            || result == JavaPrimitiveType.BOOLEAN
                                            || result == JavaPrimitiveType.BYTE
                                            || result == JavaPrimitiveType.CHAR
                                            || result == JavaPrimitiveType.SHORT;
                            case LRETURN -> result == JavaPrimitiveType.LONG;
                            case FRETURN -> result == JavaPrimitiveType.FLOAT;
                            case DRETURN -> result == JavaPrimitiveType.DOUBLE;
                            case ARETURN ->
                                    result instanceof JavaClassDescriptor
                                            || result instanceof JavaArrayDescriptor;
                            case RETURN -> result == JavaVoidType.VOID;
                            default -> throw new IllegalStateException("Unexpected return opcode");
                        };
                context.require(
                        compatible,
                        node,
                        "Return instruction does not match the method result descriptor");
            }
            default -> {}
        }
        return JvmValidationOutcome.VALID;
    }

    public static void admission(JavaInstructionNode node, JvmValidationContext context) {
        JvmOpcodeDefinition definition = JvmVocabulary.opcode(node.opcode());
        int major = context.root().version().major();
        context.require(
                major >= definition.minimumMajor()
                        && (definition.maximumMajor() == 0 || major <= definition.maximumMajor()),
                node,
                "Instruction is unavailable in this classfile version: " + node.opcode());
    }

    /** Applies to both explicit local operands and compact opcodes whose slot is in the catalog. */
    public static JvmValidationOutcome local(
            JavaInstructionNode node, JvmValidationContext context) {
        JvmOpcodeDefinition definition = JvmVocabulary.opcode(node.opcode());
        if (definition.localWidth() > 0) {
            int slot =
                    switch (node) {
                        case JavaLocalInstructionNode local -> local.slot();
                        case JavaIncrementInstructionNode increment -> increment.slot();
                        default -> definition.localSlot();
                    };
            context.require(
                    slot >= 0
                            && slot + definition.localWidth()
                                    <= context.codeLayout(node).code().maximumLocals(),
                    node,
                    "Local variable access exceeds Code.max_locals");
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome tableSwitch(
            JavaTableSwitchInstructionNode node, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(node);
        layout.boundary(
                (long) node.offset() + node.defaultDisplacement(),
                node,
                JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
        for (int displacement : node.displacements()) {
            context.session().step();
            layout.boundary(
                    (long) node.offset() + displacement,
                    node,
                    JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome lookupSwitch(
            JavaLookupSwitchInstructionNode node, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(node);
        layout.boundary(
                (long) node.offset() + node.defaultDisplacement(),
                node,
                JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
        for (var pair : node.pairs()) {
            context.session().step();
            layout.boundary(
                    (long) node.offset() + pair.displacement(),
                    node,
                    JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome interfaceCall(
            JavaInvokeInterfaceInstructionNode node, JvmValidationContext context) {
        JavaInterfaceMethodReferenceConstantNode reference =
                context.constant(
                        node.constantIndex(), JavaInterfaceMethodReferenceConstantNode.class);
        JavaNameAndTypeConstantNode nameAndType =
                context.constant(reference.nameAndTypeIndex(), JavaNameAndTypeConstantNode.class);
        int slots =
                context.method(context.utf8(nameAndType.descriptorIndex())).parameters().stream()
                                .mapToInt(parameter -> parameter.parameterSlots())
                                .sum()
                        + 1;
        context.require(
                slots == node.argumentSlots(),
                node,
                "invokeinterface count must match the descriptor's argument slots including its"
                        + " receiver");
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome dynamicCall(
            JavaInvokeDynamicInstructionNode node, JvmValidationContext context) {
        context.constant(node.constantIndex(), JavaInvokeDynamicConstantNode.class);
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome multiArray(
            JavaMultiArrayInstructionNode node, JvmValidationContext context) {
        String name = context.className(node.constantIndex());
        context.require(
                name.charAt(0) == JvmSignatureToken.ARRAY.code(),
                node,
                "multianewarray requires an array class constant");
        JavaArrayDescriptor descriptor = (JavaArrayDescriptor) context.field(name);
        context.require(
                node.dimensions() > 0 && node.dimensions() <= descriptor.dimensions(),
                node,
                "multianewarray dimensions exceed the class descriptor's rank");
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome constant(
            JavaConstantInstructionNode node, JvmValidationContext context) {
        JavaConstantNode constant = context.root().pool().entry(node.constantIndex());
        switch (node.opcode()) {
            case LDC, LDC_W, LDC2_W -> constantLoad(node, constant, context);
            case GETFIELD, GETSTATIC, PUTFIELD, PUTSTATIC ->
                    context.constant(node.constantIndex(), JavaFieldReferenceConstantNode.class);
            case INVOKEVIRTUAL, INVOKESTATIC, INVOKESPECIAL -> {
                int nameAndTypeIndex;
                if (constant instanceof JavaMethodReferenceConstantNode method) {
                    nameAndTypeIndex = method.nameAndTypeIndex();
                } else {
                    context.require(
                            node.opcode() != JvmOpcode.INVOKEVIRTUAL
                                    && context.root().version().major()
                                            >= JvmValidationVersions.INTERFACE_STATIC_SPECIAL_CALLS,
                            node,
                            "This invocation requires a class method reference");
                    nameAndTypeIndex =
                            context.constant(
                                            node.constantIndex(),
                                            JavaInterfaceMethodReferenceConstantNode.class)
                                    .nameAndTypeIndex();
                }
                JavaNameAndTypeConstantNode nameAndType =
                        context.constant(nameAndTypeIndex, JavaNameAndTypeConstantNode.class);
                String name = context.utf8(nameAndType.nameIndex());
                context.require(
                        !name.equals(JavaJvmNames.CONSTRUCTOR)
                                || node.opcode() == JvmOpcode.INVOKESPECIAL,
                        node,
                        "Only invokespecial may invoke an instance initialization method");
            }
            case NEW -> new JavaClassDescriptor(context.className(node.constantIndex()));
            case ANEWARRAY -> {
                String component = context.className(node.constantIndex());
                if (component.charAt(0) == JvmSignatureToken.ARRAY.code()) {
                    JavaArrayDescriptor array = (JavaArrayDescriptor) context.field(component);
                    context.require(
                            array.dimensions() < JavaArrayDescriptor.MAXIMUM_DIMENSIONS,
                            node,
                            "anewarray would exceed the JVM array rank limit");
                }
            }
            case CHECKCAST, INSTANCEOF -> context.className(node.constantIndex());
            default ->
                    throw context.failure(
                            node, "Unexpected constant-operand instruction: " + node.opcode());
        }
        return JvmValidationOutcome.VALID;
    }

    private static void constantLoad(
            JavaConstantInstructionNode node,
            JavaConstantNode constant,
            JvmValidationContext context) {
        int slots = loadableSlots(constant, context);
        context.require(
                slots == (node.opcode() == JvmOpcode.LDC2_W ? 2 : 1),
                node,
                "The ldc instruction does not match the constant's computational category");
    }

    public static int loadableSlots(JavaConstantNode constant, JvmValidationContext context) {
        int slots;
        if (constant instanceof JavaLongConstantNode
                || constant instanceof JavaDoubleConstantNode) {
            slots = 2;
        } else if (constant instanceof JavaIntegerConstantNode
                || constant instanceof JavaFloatConstantNode
                || constant instanceof JavaStringConstantNode
                || constant instanceof JavaMethodTypeConstantNode
                || constant instanceof JavaMethodHandleConstantNode) {
            slots = 1;
        } else if (constant instanceof JavaClassConstantNode) {
            context.require(
                    context.root().version().major() >= JvmValidationVersions.CLASS_LITERALS,
                    constant,
                    "Loading a class constant requires classfile 49 or newer");
            slots = 1;
        } else if (constant instanceof JavaDynamicConstantNode dynamic) {
            JavaNameAndTypeConstantNode nameAndType =
                    context.constant(dynamic.nameAndTypeIndex(), JavaNameAndTypeConstantNode.class);
            slots = context.field(context.utf8(nameAndType.descriptorIndex())).parameterSlots();
        } else {
            throw context.failure(constant, "The constant kind is not loadable");
        }
        return slots;
    }
}
