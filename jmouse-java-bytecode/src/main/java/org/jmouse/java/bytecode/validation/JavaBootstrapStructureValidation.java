package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.attribute.JavaBootstrapMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaBootstrapMethodsAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaMethodHandleConstantNode;
import org.jmouse.java.bytecode.vocabulary.JvmReferenceKind;

public final class JavaBootstrapStructureValidation {
    private JavaBootstrapStructureValidation() {}

    public static JvmValidationOutcome validate(
            JavaBootstrapMethodsAttributeNode node, JvmValidationContext context) {
        for (JavaBootstrapMethodNode bootstrap : node.entries()) {
            context.at(
                    bootstrap,
                    () -> {
                        JavaMethodHandleConstantNode handle =
                                context.constant(
                                        bootstrap.methodHandleIndex(),
                                        JavaMethodHandleConstantNode.class);
                        context.require(
                                handle.referenceKind() == JvmReferenceKind.REF_INVOKE_STATIC
                                        || handle.referenceKind()
                                                == JvmReferenceKind.REF_NEW_INVOKE_SPECIAL,
                                bootstrap,
                                "A bootstrap method handle requires invoke-static or"
                                        + " new-invoke-special");
                        for (int index : bootstrap.argumentIndices()) {
                            context.session().step();
                            JavaInstructionStructureValidation.loadableSlots(
                                    context.root().pool().entry(index), context);
                        }
                    });
        }
        return JvmValidationOutcome.VALID;
    }
}
