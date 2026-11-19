package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaExceptionHandlerNode;
import org.jmouse.java.bytecode.node.instruction.JavaBranchInstructionNode;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;

public final class JavaCodeStructureValidation {
    private JavaCodeStructureValidation() {}

    public static JvmValidationOutcome code(
            JavaCodeAttributeNode node, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(node);
        JavaMethodNode method = JvmValidationContext.enclosing(node, JavaMethodNode.class);
        JavaMethodDescriptor descriptor = context.method(method.descriptor());
        int slots =
                descriptor.parameters().stream().mapToInt(JavaTypeDescriptor::parameterSlots).sum()
                        + ((method.accessFlags() & JvmMethodAccessFlag.STATIC.code()) != 0 ? 0 : 1);
        context.require(
                node.maximumLocals() >= slots,
                node,
                "Code.max_locals cannot hold the method parameters and receiver");
        for (JavaExceptionHandlerNode handler : node.handlers()) {
            context.at(
                    handler,
                    () -> {
                        context.require(
                                handler.startPc() < handler.endPc(),
                                handler,
                                "An exception handler range must be non-empty");
                        layout.boundary(
                                handler.startPc(),
                                handler,
                                JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
                        layout.boundary(
                                handler.endPc(), handler, JvmCodeLayout.EndPolicy.ALLOW_END);
                        layout.boundary(
                                handler.handlerPc(),
                                handler,
                                JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
                        if (handler.catchTypeIndex() != 0) {
                            new JavaClassDescriptor(context.className(handler.catchTypeIndex()));
                        }
                    });
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome branch(
            JavaBranchInstructionNode node, JvmValidationContext context) {
        context.codeLayout(node)
                .boundary(
                        (long) node.offset() + node.displacement(),
                        node,
                        JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
        return JvmValidationOutcome.VALID;
    }
}
