package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.JavaMemberNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleAttributeNode;
import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JavaClassStructureValidation {
    private record MemberIdentity(String name, String descriptor) {}

    private JavaClassStructureValidation() {}

    public static JvmValidationOutcome validate(
            JavaClassFileNode node, JvmValidationContext context) {
        new JavaClassDescriptor(node.internalName());
        boolean isModule = (node.accessFlags() & JvmClassAccessFlag.MODULE.code()) != 0;
        if (isModule) {
            context.require(
                    node.version().major() >= JvmValidationVersions.MODULES
                            && node.accessFlags() == JvmClassAccessFlag.MODULE.code()
                            && node.internalName().equals(JavaJvmNames.MODULE_DESCRIPTOR)
                            && node.superclassIndex() == 0
                            && node.interfaceIndices().isEmpty()
                            && node.fields().isEmpty()
                            && node.methods().isEmpty(),
                    node,
                    "Invalid module descriptor class header");
            context.require(
                    node.attributes().stream()
                                    .filter(JavaModuleAttributeNode.class::isInstance)
                                    .count()
                            == 1,
                    node,
                    "A module descriptor requires exactly one Module attribute");
        } else {
            context.require(
                    node.superclassIndex() != 0 || node.internalName().equals(JavaJvmNames.OBJECT),
                    node,
                    "Only java/lang/Object and module descriptors may omit a superclass");
            context.require(
                    !node.internalName().equals(JavaJvmNames.OBJECT) || node.superclassIndex() == 0,
                    node,
                    "java/lang/Object must omit its superclass");
            if (node.superclassIndex() != 0) {
                new JavaClassDescriptor(context.className(node.superclassIndex()));
            }
        }
        if (context.isInterface()) {
            int forbidden =
                    JvmClassAccessFlag.FINAL.code()
                            | JvmClassAccessFlag.SUPER.code()
                            | JvmClassAccessFlag.ENUM.code()
                            | JvmClassAccessFlag.MODULE.code();
            context.require(
                    (node.accessFlags() & JvmClassAccessFlag.ABSTRACT.code()) != 0
                            && (node.accessFlags() & forbidden) == 0,
                    node,
                    "Invalid interface class flags");
            context.require(
                    node.superclassIndex() != 0
                            && context.className(node.superclassIndex())
                                    .equals(JavaJvmNames.OBJECT),
                    node,
                    "An interface's classfile superclass must be java/lang/Object");
        }
        context.require(
                (node.accessFlags() & JvmClassAccessFlag.ANNOTATION.code()) == 0
                        || context.isInterface(),
                node,
                "An annotation class must be an interface");
        context.require(
                (node.accessFlags()
                                & (JvmClassAccessFlag.FINAL.code()
                                        | JvmClassAccessFlag.ABSTRACT.code()))
                        != (JvmClassAccessFlag.FINAL.code() | JvmClassAccessFlag.ABSTRACT.code()),
                node,
                "A class cannot be both final and abstract");
        Set<String> interfaces = new HashSet<>();
        for (int index : node.interfaceIndices()) {
            String name = context.className(index);
            new JavaClassDescriptor(name);
            context.require(interfaces.add(name), node, "Duplicate direct interface: " + name);
        }
        uniqueMembers(node.fields(), context);
        uniqueMembers(node.methods(), context);
        return JvmValidationOutcome.VALID;
    }

    private static void uniqueMembers(
            List<? extends JavaMemberNode> members, JvmValidationContext context) {
        Set<MemberIdentity> identities = new HashSet<>();
        for (JavaMemberNode member : members) {
            context.session().step();
            context.require(
                    identities.add(new MemberIdentity(member.name(), member.descriptor())),
                    member,
                    "Duplicate JVM member name and descriptor");
        }
    }
}
