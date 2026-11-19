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
                        context.constant(bootstrap.methodHandleIndex(), JavaMethodHandleConstantNode.class);
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
