package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.node.attribute.JavaBootstrapMethodsAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaFieldReferenceConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaInterfaceMethodReferenceConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaMethodHandleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaMethodReferenceConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaNameAndTypeConstantNode;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.type.JavaVoidType;

public final class JavaConstantStructureValidation {
    private JavaConstantStructureValidation() {}

    public static void nameAndType(JavaNameAndTypeConstantNode node, JvmValidationContext context) {
        String name = context.utf8(node.nameIndex());
        String descriptor = context.utf8(node.descriptorIndex());
        if (!descriptor.isEmpty()
                && descriptor.charAt(0) == JvmSignatureToken.PARAMETERS_OPEN.code()) {
            context.methodName(name);
            JavaMethodDescriptor method = context.method(descriptor);
            if (name.equals(JavaJvmNames.CONSTRUCTOR)) {
                context.require(
                        method.result() == JavaVoidType.VOID,
                        node,
                        "A constructor descriptor must return void");
            }
        } else {
            context.unqualifiedName(name);
            context.field(descriptor);
        }
    }

    public static void fieldReference(
            int ownerIndex, int nameAndTypeIndex, JvmValidationContext context) {
        context.className(ownerIndex);
        JavaNameAndTypeConstantNode nameAndType =
                context.constant(nameAndTypeIndex, JavaNameAndTypeConstantNode.class);
        context.unqualifiedName(context.utf8(nameAndType.nameIndex()));
        context.field(context.utf8(nameAndType.descriptorIndex()));
    }

    public static void methodReference(
            JavaBytecodeNode node,
            int ownerIndex,
            int nameAndTypeIndex,
            JvmValidationContext context) {
        context.className(ownerIndex);
        JavaNameAndTypeConstantNode nameAndType =
                context.constant(nameAndTypeIndex, JavaNameAndTypeConstantNode.class);
        String name = context.utf8(nameAndType.nameIndex());
        context.methodName(name);
        JavaMethodDescriptor descriptor =
                context.method(context.utf8(nameAndType.descriptorIndex()));
        context.require(
                !name.equals(JavaJvmNames.CLASS_INITIALIZER),
                node,
                "A symbolic reference cannot target a class initializer");
        if (name.equals(JavaJvmNames.CONSTRUCTOR)) {
            context.require(
                    node instanceof JavaMethodReferenceConstantNode
                            && descriptor.result() == JavaVoidType.VOID,
                    node,
                    "Invalid symbolic constructor reference");
        }
    }

    public static void dynamic(
            JavaBytecodeNode node,
            int bootstrapIndex,
            int nameAndTypeIndex,
            JvmValidationContext context,
            DynamicKind kind) {
        int minimum =
                kind == DynamicKind.CONSTANT
                        ? JvmValidationVersions.DYNAMIC_CONSTANTS
                        : JvmValidationVersions.METHOD_HANDLES;
        context.require(
                context.root().version().major() >= minimum,
                node,
                "Dynamic constant/call is unavailable in this classfile version");
        JavaNameAndTypeConstantNode nameAndType =
                context.constant(nameAndTypeIndex, JavaNameAndTypeConstantNode.class);
        String name = context.utf8(nameAndType.nameIndex());
        context.require(
                !name.equals(JavaJvmNames.CONSTRUCTOR)
                        && !name.equals(JavaJvmNames.CLASS_INITIALIZER),
                node,
                "A dynamic constant/call cannot use an initialization method name");
        if (kind == DynamicKind.CONSTANT) {
            context.unqualifiedName(name);
            context.field(context.utf8(nameAndType.descriptorIndex()));
        } else {
            context.methodName(name);
            context.method(context.utf8(nameAndType.descriptorIndex()));
        }
        JavaBootstrapMethodsAttributeNode methods =
                context.root().attributes().stream()
                        .filter(JavaBootstrapMethodsAttributeNode.class::isInstance)
                        .map(JavaBootstrapMethodsAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        context.failure(
                                                node, "Dynamic entry requires BootstrapMethods"));
        context.require(
                bootstrapIndex >= 0 && bootstrapIndex < methods.entries().size(),
                node,
                "Invalid bootstrap method table index");
    }

    public enum DynamicKind {
        CONSTANT,
        CALL
    }

    public static void methodHandle(
            JavaMethodHandleConstantNode node, JvmValidationContext context) {
        context.require(
                context.root().version().major() >= JvmValidationVersions.METHOD_HANDLES,
                node,
                "Method handles are unavailable in this classfile version");
        JavaConstantNode target = context.root().pool().entry(node.referenceIndex());
        int nameAndTypeIndex =
                switch (node.referenceKind()) {
                    case REF_GET_FIELD, REF_GET_STATIC, REF_PUT_FIELD, REF_PUT_STATIC ->
                            context.constant(
                                            node.referenceIndex(),
                                            JavaFieldReferenceConstantNode.class)
                                    .nameAndTypeIndex();
                    case REF_INVOKE_VIRTUAL, REF_NEW_INVOKE_SPECIAL ->
                            context.constant(
                                            node.referenceIndex(),
                                            JavaMethodReferenceConstantNode.class)
                                    .nameAndTypeIndex();
                    case REF_INVOKE_INTERFACE ->
                            context.constant(
                                            node.referenceIndex(),
                                            JavaInterfaceMethodReferenceConstantNode.class)
                                    .nameAndTypeIndex();
                    case REF_INVOKE_STATIC, REF_INVOKE_SPECIAL -> {
                        if (target instanceof JavaMethodReferenceConstantNode method) {
                            yield method.nameAndTypeIndex();
                        }
                        context.require(
                                context.root().version().major()
                                        >= JvmValidationVersions.INTERFACE_STATIC_SPECIAL_CALLS,
                                node,
                                "Interface static/special method handles require classfile 52 or"
                                        + " newer");
                        yield context.constant(
                                        node.referenceIndex(),
                                        JavaInterfaceMethodReferenceConstantNode.class)
                                .nameAndTypeIndex();
                    }
                };
        JavaNameAndTypeConstantNode nameAndType =
                context.constant(nameAndTypeIndex, JavaNameAndTypeConstantNode.class);
        String name = context.utf8(nameAndType.nameIndex());
        switch (node.referenceKind()) {
            case REF_GET_FIELD, REF_GET_STATIC, REF_PUT_FIELD, REF_PUT_STATIC -> {}
            case REF_NEW_INVOKE_SPECIAL ->
                    context.require(
                            name.equals(JavaJvmNames.CONSTRUCTOR),
                            node,
                            "A new-invoke-special method handle must reference a constructor");
            default ->
                    context.require(
                            !name.equals(JavaJvmNames.CONSTRUCTOR)
                                    && !name.equals(JavaJvmNames.CLASS_INITIALIZER),
                            node,
                            "An ordinary invocation method handle cannot reference an"
                                    + " initialization method");
        }
    }
}
