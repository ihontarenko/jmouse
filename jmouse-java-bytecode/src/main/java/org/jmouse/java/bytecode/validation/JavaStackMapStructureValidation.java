package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.JavaBytecodeMode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaObjectVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaUninitializedVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaVerificationTypeNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;
import org.jmouse.java.model.symbol.JavaJvmNames;

import java.util.ArrayList;
import java.util.List;

public final class JavaStackMapStructureValidation {
    private JavaStackMapStructureValidation() {}

    public static JvmValidationOutcome validate(
            JavaStackMapTableAttributeNode node, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(node);
        JavaMethodNode method = JvmValidationContext.enclosing(node, JavaMethodNode.class);
        List<Integer> locals = new ArrayList<>();
        if (!method.name().equals(JavaJvmNames.CLASS_INITIALIZER)
                && (method.accessFlags() & JvmMethodAccessFlag.STATIC.code()) == 0) {
            locals.add(1);
        }
        context.method(method.descriptor())
                .parameters()
                .forEach(parameter -> locals.add(parameter.parameterSlots()));
        long previousOffset = -1;
        for (JavaStackMapFrameNode frame : node.entries()) {
            context.session().step();
            long offset = previousOffset + frame.offsetDelta() + 1;
            layout.boundary(offset, frame, JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
            switch (frame.kind()) {
                case CHOP_FRAME -> {
                    context.require(
                            frame.choppedLocals() <= locals.size(),
                            frame,
                            "A chop frame removes more locals than exist");
                    locals.subList(locals.size() - frame.choppedLocals(), locals.size()).clear();
                }
                case FULL_FRAME -> {
                    locals.clear();
                    appendLocals(frame, locals, layout, method, context);
                }
                case APPEND_FRAME -> appendLocals(frame, locals, layout, method, context);
                case SAME_FRAME,
                        SAME_FRAME_EXTENDED,
                        SAME_LOCALS_ONE_STACK_ITEM_FRAME,
                        SAME_LOCALS_ONE_STACK_ITEM_FRAME_EXTENDED -> {}
                case RESERVED ->
                        throw context.failure(
                                frame, "A reserved stack map frame is not a valid frame");
            }
            context.require(
                    locals.stream().mapToInt(Integer::intValue).sum()
                            <= layout.code().maximumLocals(),
                    frame,
                    "Stack map locals exceed Code.max_locals");
            int stackSlots = 0;
            for (JavaVerificationTypeNode type : frame.stack()) {
                validateType(type, layout, method, context);
                stackSlots += slots(type);
            }
            context.require(
                    stackSlots <= layout.code().maximumStack(),
                    frame,
                    "Stack map stack exceeds Code.max_stack");
            previousOffset = offset;
        }
        return JvmValidationOutcome.VALID;
    }

    private static void appendLocals(
            JavaStackMapFrameNode frame,
            List<Integer> locals,
            JvmCodeLayout layout,
            JavaMethodNode method,
            JvmValidationContext context) {
        for (JavaVerificationTypeNode type : frame.locals()) {
            validateType(type, layout, method, context);
            locals.add(slots(type));
        }
    }

    private static int slots(JavaVerificationTypeNode type) {
        return type.tag() == JvmVerificationTypeTag.LONG
                        || type.tag() == JvmVerificationTypeTag.DOUBLE
                ? 2
                : 1;
    }

    private static void validateType(
            JavaVerificationTypeNode type,
            JvmCodeLayout layout,
            JavaMethodNode method,
            JvmValidationContext context) {
        context.at(
                type,
                () -> {
                    if (type instanceof JavaObjectVerificationTypeNode object) {
                        context.className(object.classIndex());
                    } else if (type
                            instanceof JavaUninitializedVerificationTypeNode uninitialized) {
                        layout.boundary(
                                uninitialized.newOffset(),
                                type,
                                JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
                        if (context.mode() != JavaBytecodeMode.DECLARATIONS_ONLY) {
                            JavaInstructionNode instruction =
                                    layout.instruction(uninitialized.newOffset());
                            context.require(
                                    instruction.opcode() == JvmOpcode.NEW,
                                    type,
                                    "An uninitialized verification type must reference a new"
                                            + " instruction");
                        }
                    } else if (type.tag() == JvmVerificationTypeTag.UNINITIALIZED_THIS) {
                        context.require(
                                method.name().equals(JavaJvmNames.CONSTRUCTOR),
                                type,
                                "UninitializedThis is valid only in an instance initialization"
                                        + " method");
                    }
                });
    }
}
