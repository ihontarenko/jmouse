package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.attribute.JavaLineNumberNode;
import org.jmouse.java.bytecode.node.attribute.JavaLineNumberTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTypeTableAttributeNode;

public final class JavaDebugTableStructureValidation {
    private JavaDebugTableStructureValidation() {}

    public static JvmValidationOutcome lines(
            JavaLineNumberTableAttributeNode node, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(node);
        for (JavaLineNumberNode line : node.entries()) {
            context.session().step();
            layout.boundary(line.startPc(), line, JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome locals(
            JavaLocalVariableTableAttributeNode node, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(node);
        for (JavaLocalVariableNode local : node.entries()) {
            context.at(
                    local,
                    () -> {
                        int width = context.field(context.utf8(local.typeIndex())).parameterSlots();
                        localRange(local, width, layout, context);
                    });
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome genericLocals(
            JavaLocalVariableTypeTableAttributeNode node, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(node);
        for (JavaLocalVariableNode local : node.entries()) {
            context.at(
                    local,
                    () -> {
                        context.referenceSignature(context.utf8(local.typeIndex()));
                        localRange(local, 1, layout, context);
                    });
        }
        return JvmValidationOutcome.VALID;
    }

    private static void localRange(
            JavaLocalVariableNode local,
            int width,
            JvmCodeLayout layout,
            JvmValidationContext context) {
        layout.boundary(local.startPc(), local, JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
        layout.boundary(
                (long) local.startPc() + local.length(), local, JvmCodeLayout.EndPolicy.ALLOW_END);
        context.unqualifiedName(context.utf8(local.nameIndex()));
        context.require(
                local.slot() + width <= layout.code().maximumLocals(),
                local,
                "A debug local variable exceeds Code.max_locals");
    }
}
