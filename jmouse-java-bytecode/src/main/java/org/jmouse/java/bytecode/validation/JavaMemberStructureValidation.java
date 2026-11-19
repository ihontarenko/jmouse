package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.JavaFieldNode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.vocabulary.JvmFieldAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.type.JavaVoidType;

public final class JavaMemberStructureValidation {
    private JavaMemberStructureValidation() {}

    public static JvmValidationOutcome field(JavaFieldNode node, JvmValidationContext context) {
        context.unqualifiedName(node.name());
        context.field(node.descriptor());
        int flags = node.accessFlags();
        int visibility =
                flags
                        & (JvmFieldAccessFlag.PUBLIC.code()
                                | JvmFieldAccessFlag.PRIVATE.code()
                                | JvmFieldAccessFlag.PROTECTED.code());
        context.require(
                Integer.bitCount(visibility) <= 1, node, "Conflicting field visibility flags");
        context.require(
                (flags & (JvmFieldAccessFlag.FINAL.code() | JvmFieldAccessFlag.VOLATILE.code()))
                        != (JvmFieldAccessFlag.FINAL.code() | JvmFieldAccessFlag.VOLATILE.code()),
                node,
                "A field cannot be both final and volatile");
        if (context.isInterface()) {
            int required =
                    JvmFieldAccessFlag.PUBLIC.code()
                            | JvmFieldAccessFlag.STATIC.code()
                            | JvmFieldAccessFlag.FINAL.code();
            int forbidden =
                    JvmFieldAccessFlag.PRIVATE.code()
                            | JvmFieldAccessFlag.PROTECTED.code()
                            | JvmFieldAccessFlag.VOLATILE.code()
                            | JvmFieldAccessFlag.TRANSIENT.code();
            context.require(
                    (flags & required) == required && (flags & forbidden) == 0,
                    node,
                    "Invalid interface field access flags");
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome method(JavaMethodNode node, JvmValidationContext context) {
        context.methodName(node.name());
        JavaMethodDescriptor descriptor = context.method(node.descriptor());
        int flags = node.accessFlags();
        int visibility =
                flags
                        & (JvmMethodAccessFlag.PUBLIC.code()
                                | JvmMethodAccessFlag.PRIVATE.code()
                                | JvmMethodAccessFlag.PROTECTED.code());
        context.require(
                Integer.bitCount(visibility) <= 1, node, "Conflicting method visibility flags");
        boolean isStatic = (flags & JvmMethodAccessFlag.STATIC.code()) != 0;
        int slots =
                descriptor.parameters().stream().mapToInt(JavaTypeDescriptor::parameterSlots).sum()
                        + (isStatic ? 0 : 1);
        context.require(
                slots <= JavaMethodDescriptor.MAXIMUM_PARAMETER_SLOTS,
                node,
                "Method parameters including the receiver exceed the JVM slot limit");
        boolean isAbstract = (flags & JvmMethodAccessFlag.ABSTRACT.code()) != 0;
        boolean isNative = (flags & JvmMethodAccessFlag.NATIVE.code()) != 0;
        long codeCount =
                node.attributes().stream().filter(JavaCodeAttributeNode.class::isInstance).count();
        context.require(
                codeCount == (isAbstract || isNative ? 0 : 1),
                node,
                "A concrete method requires exactly one Code attribute; abstract/native methods"
                        + " forbid Code");
        if (isAbstract) {
            int forbidden =
                    JvmMethodAccessFlag.PRIVATE.code()
                            | JvmMethodAccessFlag.STATIC.code()
                            | JvmMethodAccessFlag.FINAL.code()
                            | JvmMethodAccessFlag.SYNCHRONIZED.code()
                            | JvmMethodAccessFlag.NATIVE.code();
            context.require((flags & forbidden) == 0, node, "Invalid abstract method access flags");
        }
        if (node.name().equals(JavaJvmNames.CONSTRUCTOR)) {
            context.require(
                    !context.isInterface()
                            && !isStatic
                            && !isAbstract
                            && !isNative
                            && descriptor.result() == JavaVoidType.VOID,
                    node,
                    "Invalid instance initialization method");
            int forbidden =
                    JvmMethodAccessFlag.FINAL.code()
                            | JvmMethodAccessFlag.SYNCHRONIZED.code()
                            | JvmMethodAccessFlag.BRIDGE.code();
            context.require((flags & forbidden) == 0, node, "Invalid constructor access flags");
        }
        if (node.name().equals(JavaJvmNames.CLASS_INITIALIZER)) {
            context.require(
                    descriptor.parameters().isEmpty() && descriptor.result() == JavaVoidType.VOID,
                    node,
                    "A class initializer requires the ()V descriptor");
            context.require(
                    context.root().version().major()
                                    < JvmValidationVersions.STATIC_CLASS_INITIALIZER
                            || isStatic,
                    node,
                    "A class initializer must be static");
        }
        return JvmValidationOutcome.VALID;
    }
}
