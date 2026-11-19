package org.jmouse.java.bytecode.validation;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.bytecode.node.JavaFieldNode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.*;
import org.jmouse.java.bytecode.node.constant.*;
import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmMethodParameterAccessFlag;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.type.JavaPrimitiveType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Constraints owned by an attribute, independent from instruction decoding and type-flow analysis.
 */
public final class JavaAttributeStructureValidation {
    private JavaAttributeStructureValidation() {}

    public static void install(
            NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> rules) {
        bind(
                rules,
                JavaConstantValueAttributeNode.KIND,
                JavaAttributeStructureValidation::constantValue);
        bind(
                rules,
                JavaExceptionsAttributeNode.KIND,
                (node, context) -> classIndices(node, node.exceptionIndices(), context));
        bind(
                rules,
                JavaSourceFileAttributeNode.KIND,
                (node, context) -> context.utf8(node.sourceFileIndex()));
        bind(
                rules,
                JavaInnerClassesAttributeNode.KIND,
                JavaAttributeStructureValidation::innerClasses);
        bind(
                rules,
                JavaEnclosingMethodAttributeNode.KIND,
                JavaAttributeStructureValidation::enclosingMethod);
        bind(
                rules,
                JavaNestHostAttributeNode.KIND,
                (node, context) -> {
                    new JavaClassDescriptor(context.className(node.hostClassIndex()));
                    context.require(
                            context.root().attributes().stream()
                                    .noneMatch(JavaNestMembersAttributeNode.class::isInstance),
                            node,
                            "NestHost and NestMembers are mutually exclusive");
                });
        bind(
                rules,
                JavaNestMembersAttributeNode.KIND,
                (node, context) -> classIndices(node, node.classIndices(), context));
        bind(
                rules,
                JavaPermittedSubclassesAttributeNode.KIND,
                (node, context) -> {
                    context.require(
                            (context.root().accessFlags() & JvmClassAccessFlag.FINAL.code()) == 0,
                            node,
                            "A final class cannot declare PermittedSubclasses");
                    classIndices(node, node.classIndices(), context);
                });
        bind(rules, JavaRecordAttributeNode.KIND, JavaAttributeStructureValidation::record);
        bind(
                rules,
                JavaMethodParametersAttributeNode.KIND,
                JavaAttributeStructureValidation::methodParameters);
        // These payloads are fully checked by their decoder and the attribute table's metadata.
        bind(rules, JavaSignatureAttributeNode.KIND, (node, context) -> {});
        bind(rules, JavaSyntheticAttributeNode.KIND, (node, context) -> {});
        bind(rules, JavaDeprecatedAttributeNode.KIND, (node, context) -> {});
        bind(rules, JavaSourceDebugExtensionAttributeNode.KIND, (node, context) -> {});
        // Unrecognized attributes are explicitly preserved and ignored, as required by the JVM
        // format.
        bind(rules, JavaUnknownAttributeNode.KIND, (node, context) -> {});
    }

    private static void constantValue(
            JavaConstantValueAttributeNode node, JvmValidationContext context) {
        JavaFieldNode field = JvmValidationContext.enclosing(node, JavaFieldNode.class);
        JavaTypeDescriptor descriptor = context.field(field.descriptor());
        JavaConstantNode constant = context.root().pool().entry(node.constantIndex());
        boolean compatible;
        if (descriptor instanceof JavaPrimitiveType primitive) {
            compatible =
                    switch (primitive) {
                        case BOOLEAN, BYTE, CHAR, SHORT, INT ->
                                constant instanceof JavaIntegerConstantNode;
                        case LONG -> constant instanceof JavaLongConstantNode;
                        case FLOAT -> constant instanceof JavaFloatConstantNode;
                        case DOUBLE -> constant instanceof JavaDoubleConstantNode;
                    };
        } else {
            compatible =
                    descriptor instanceof JavaClassDescriptor type
                            && type.internalName().equals(JavaJvmNames.STRING)
                            && constant instanceof JavaStringConstantNode;
        }
        context.require(compatible, node, "ConstantValue does not match the field descriptor");
    }

    private static void classIndices(
            JavaAttributeNode node, List<Integer> indices, JvmValidationContext context) {
        Set<String> classes = new HashSet<>();
        for (int index : indices) {
            context.session().step();
            String name = context.className(index);
            new JavaClassDescriptor(name);
            context.require(classes.add(name), node, "Duplicate class in " + node.name());
        }
    }

    private static void innerClasses(
            JavaInnerClassesAttributeNode node, JvmValidationContext context) {
        Set<String> classes = new HashSet<>();
        for (JavaInnerClassNode entry : node.entries()) {
            context.at(
                    entry,
                    () -> {
                        String name = context.className(entry.innerClassIndex());
                        new JavaClassDescriptor(name);
                        context.require(classes.add(name), entry, "Duplicate InnerClasses entry");
                        context.require(
                                entry.outerClassIndex() != entry.innerClassIndex(),
                                entry,
                                "An InnerClasses entry cannot name itself as its outer class");
                        context.require(
                                context.root().version().major()
                                                < JvmValidationVersions.STATIC_CLASS_INITIALIZER
                                        || entry.innerNameIndex() != 0
                                        || entry.outerClassIndex() == 0,
                                entry,
                                "An anonymous inner class cannot declare an outer-class index");
                        if (entry.outerClassIndex() != 0) {
                            new JavaClassDescriptor(context.className(entry.outerClassIndex()));
                        }
                        if (entry.innerNameIndex() != 0) {
                            context.unqualifiedName(context.utf8(entry.innerNameIndex()));
                        }
                    });
        }
    }

    private static void enclosingMethod(
            JavaEnclosingMethodAttributeNode node, JvmValidationContext context) {
        new JavaClassDescriptor(context.className(node.classIndex()));
        if (node.methodIndex() != 0) {
            JavaNameAndTypeConstantNode nameAndType =
                    context.constant(node.methodIndex(), JavaNameAndTypeConstantNode.class);
            context.methodName(context.utf8(nameAndType.nameIndex()));
            context.method(context.utf8(nameAndType.descriptorIndex()));
        }
    }

    private static void record(JavaRecordAttributeNode node, JvmValidationContext context) {
        Set<String> names = new HashSet<>();
        for (JavaRecordComponentNode component : node.entries()) {
            context.at(
                    component,
                    () -> {
                        String name = context.utf8(component.nameIndex());
                        context.unqualifiedName(name);
                        context.require(
                                names.add(name), component, "Duplicate record component name");
                        context.field(context.utf8(component.descriptorIndex()));
                    });
        }
    }

    private static void methodParameters(
            JavaMethodParametersAttributeNode node, JvmValidationContext context) {
        JavaMethodNode method = JvmValidationContext.enclosing(node, JavaMethodNode.class);
        context.require(
                node.entries().size() == context.method(method.descriptor()).parameters().size(),
                node,
                "MethodParameters count must match the method descriptor");
        int allowed =
                JvmMethodParameterAccessFlag.FINAL.code()
                        | JvmMethodParameterAccessFlag.SYNTHETIC.code()
                        | JvmMethodParameterAccessFlag.MANDATED.code();
        for (JavaMethodParameterNode entry : node.entries()) {
            context.at(
                    entry,
                    () -> {
                        if (entry.nameIndex() != 0) {
                            context.unqualifiedName(context.utf8(entry.nameIndex()));
                        }
                        context.require(
                                (entry.accessFlags() & ~allowed) == 0,
                                entry,
                                "Invalid method parameter access flags");
                    });
        }
    }

    private static <N extends JavaAttributeNode> void bind(
            NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> rules,
            NodeKind<N> kind,
            BiConsumer<N, JvmValidationContext> validation) {
        rules.bind(
                kind,
                JavaAttributeStructureValidation.class.getName(),
                (node, context) -> {
                    validation.accept(node, context);
                    return JvmValidationOutcome.VALID;
                });
    }
}
