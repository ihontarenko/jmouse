package org.jmouse.java.bytecode.validation;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.JavaFieldNode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.*;
import org.jmouse.java.bytecode.signature.*;
import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmFormatLimits;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public final class JavaAnnotationStructureValidation {
    private JavaAnnotationStructureValidation() {}

    public static void install(
            NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> rules) {
        bind(rules, JavaRuntimeVisibleAnnotationsAttributeNode.KIND, (node, context) -> {});
        bind(rules, JavaRuntimeInvisibleAnnotationsAttributeNode.KIND, (node, context) -> {});
        bind(rules, JavaRuntimeVisibleTypeAnnotationsAttributeNode.KIND, (node, context) -> {});
        bind(rules, JavaRuntimeInvisibleTypeAnnotationsAttributeNode.KIND, (node, context) -> {});
        bind(
                rules,
                JavaRuntimeVisibleParameterAnnotationsAttributeNode.KIND,
                (node, context) -> parameterCount(node, node.entries().size(), context));
        bind(
                rules,
                JavaRuntimeInvisibleParameterAnnotationsAttributeNode.KIND,
                (node, context) -> parameterCount(node, node.entries().size(), context));
        bind(
                rules,
                JavaAnnotationDefaultAttributeNode.KIND,
                (node, context) -> {
                    JavaMethodNode method =
                            JvmValidationContext.enclosing(node, JavaMethodNode.class);
                    context.require(
                            (context.root().accessFlags() & JvmClassAccessFlag.ANNOTATION.code())
                                            != 0
                                    && context.method(method.descriptor()).parameters().isEmpty(),
                            node,
                            "AnnotationDefault belongs to an annotation element method");
                });
        bind(rules, JavaAnnotationNode.KIND, JavaAnnotationStructureValidation::annotation);
        // The decoder checks the tag/constant-kind pair, and the common walker visits nested
        // values.
        bind(rules, JavaConstantAnnotationValueNode.KIND, (node, context) -> {});
        bind(rules, JavaNestedAnnotationValueNode.KIND, (node, context) -> {});
        bind(rules, JavaArrayAnnotationValueNode.KIND, (node, context) -> {});
        bind(
                rules,
                JavaEnumAnnotationValueNode.KIND,
                (node, context) -> {
                    classDescriptor(context.utf8(node.typeNameIndex()), node, context);
                    context.unqualifiedName(context.utf8(node.constantNameIndex()));
                });
        bind(
                rules,
                JavaClassAnnotationValueNode.KIND,
                (node, context) -> {
                    String text = context.utf8(node.classInfoIndex());
                    if (!text.equals(Character.toString(JvmSignatureToken.VOID.code()))) {
                        context.field(text);
                    }
                });
        bind(rules, JavaTypeAnnotationNode.KIND, JavaAnnotationStructureValidation::typeAnnotation);
    }

    private static void parameterCount(
            JavaAttributeNode node, int count, JvmValidationContext context) {
        JavaMethodNode method = JvmValidationContext.enclosing(node, JavaMethodNode.class);
        context.require(
                count <= context.method(method.descriptor()).parameters().size(),
                node,
                "Parameter annotation count exceeds the method descriptor's parameter count");
    }

    private static void classDescriptor(
            String text, JavaBytecodeNode node, JvmValidationContext context) {
        context.require(
                context.field(text) instanceof JavaClassDescriptor,
                node,
                "An annotation or enum type requires a class descriptor");
    }

    private static void annotation(JavaAnnotationNode node, JvmValidationContext context) {
        classDescriptor(context.utf8(node.typeIndex()), node, context);
        Set<String> names = new HashSet<>();
        for (JavaAnnotationElementNode element : node.entries()) {
            context.at(
                    element,
                    () -> {
                        String name = context.utf8(element.nameIndex());
                        context.unqualifiedName(name);
                        context.require(
                                names.add(name), element, "Duplicate annotation element name");
                    });
        }
    }

    private static void typeAnnotation(JavaTypeAnnotationNode node, JvmValidationContext context) {
        JavaAttributeNode attribute = JvmValidationContext.enclosing(node, JavaAttributeNode.class);
        Node owner = attribute.getParent();
        JavaTypeAnnotationTargetNode target = node.target();
        boolean correctOwner =
                switch (target.targetType()) {
                    case CLASS_TYPE_PARAMETER, SUPERTYPE, CLASS_TYPE_PARAMETER_BOUND ->
                            owner instanceof JavaClassFileNode;
                    case FIELD ->
                            owner instanceof JavaFieldNode
                                    || owner instanceof JavaRecordComponentNode;
                    case METHOD_TYPE_PARAMETER,
                                    METHOD_TYPE_PARAMETER_BOUND,
                                    METHOD_RETURN,
                                    METHOD_RECEIVER,
                                    METHOD_FORMAL_PARAMETER,
                                    THROWS ->
                            owner instanceof JavaMethodNode;
                    default -> owner instanceof JavaCodeAttributeNode;
                };
        context.require(
                correctOwner,
                target,
                "Type annotation target does not match its owning declaration or Code");
        switch (target) {
            case JavaSupertypeTargetNode supertype ->
                    context.require(
                            supertype.supertypeIndex() == JvmFormatLimits.SUPERCLASS_TYPE_ANNOTATION
                                    ? context.root().superclassIndex() != 0
                                    : supertype.supertypeIndex()
                                            < context.root().interfaceIndices().size(),
                            target,
                            "Invalid annotated supertype index");
            case JavaFormalParameterTargetNode parameter -> {
                JavaMethodNode method = (JavaMethodNode) owner;
                context.require(
                        parameter.parameterIndex()
                                < context.method(method.descriptor()).parameters().size(),
                        target,
                        "Invalid annotated method parameter index");
            }
            case JavaThrowsTargetNode thrown -> {
                JavaMethodNode method = (JavaMethodNode) owner;
                int count =
                        method.attributes().stream()
                                .filter(JavaExceptionsAttributeNode.class::isInstance)
                                .map(JavaExceptionsAttributeNode.class::cast)
                                .mapToInt(exceptions -> exceptions.exceptionIndices().size())
                                .findFirst()
                                .orElse(0);
                context.require(
                        thrown.throwsIndex() < count, target, "Invalid annotated throws index");
            }
            case JavaCatchTargetNode caught ->
                    context.require(
                            caught.exceptionIndex()
                                    < context.codeLayout(node).code().handlers().size(),
                            target,
                            "Invalid annotated exception table index");
            case JavaOffsetTargetNode offset ->
                    context.codeLayout(node)
                            .boundary(
                                    offset.offset(),
                                    target,
                                    JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
            case JavaTypeArgumentTargetNode argument ->
                    context.codeLayout(node)
                            .boundary(
                                    argument.offset(),
                                    target,
                                    JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
            case JavaLocalVariableTargetNode local -> localTarget(local, context);
            case JavaTypeParameterTargetNode parameter ->
                    typeParameter(owner, parameter.parameterIndex(), context, target);
            case JavaTypeParameterBoundTargetNode bound -> {
                JvmTypeParameterNode parameter =
                        typeParameter(owner, bound.parameterIndex(), context, target);
                context.require(
                        bound.boundIndex() == 0
                                ? parameter.classBound().isPresent()
                                : bound.boundIndex() <= parameter.interfaceBounds().size(),
                        target,
                        "Invalid annotated type parameter bound index");
            }
            default -> {}
        }
        if (target.targetType() == JvmTypeAnnotationTarget.METHOD_RECEIVER) {
            context.require(
                    (((JavaMethodNode) owner).accessFlags() & JvmMethodAccessFlag.STATIC.code())
                            == 0,
                    target,
                    "A static method has no receiver type to annotate");
        }
    }

    private static JvmTypeParameterNode typeParameter(
            Node owner,
            int index,
            JvmValidationContext context,
            JavaTypeAnnotationTargetNode target) {
        List<JavaAttributeNode> attributes =
                owner instanceof JavaClassFileNode type
                        ? type.attributes()
                        : ((JavaMethodNode) owner).attributes();
        JvmTextNode syntax =
                attributes.stream()
                        .filter(JavaSignatureAttributeNode.class::isInstance)
                        .map(JavaSignatureAttributeNode.class::cast)
                        .map(JavaSignatureAttributeNode::syntax)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        context.failure(
                                                target,
                                                "Type parameter annotation requires generic"
                                                        + " signature metadata"));
        List<JvmTypeParameterNode> parameters =
                syntax instanceof JvmClassSignatureNode type
                        ? type.typeParameters()
                        : ((JvmMethodSignatureNode) syntax).typeParameters();
        context.require(
                index < parameters.size(), target, "Invalid annotated type parameter index");
        return parameters.get(index);
    }

    private static void localTarget(
            JavaLocalVariableTargetNode target, JvmValidationContext context) {
        JvmCodeLayout layout = context.codeLayout(target);
        for (JavaLocalVariableTargetEntryNode entry : target.entries()) {
            context.session().step();
            layout.boundary(entry.startPc(), entry, JvmCodeLayout.EndPolicy.INSTRUCTION_ONLY);
            layout.boundary(
                    (long) entry.startPc() + entry.length(),
                    entry,
                    JvmCodeLayout.EndPolicy.ALLOW_END);
            context.require(
                    entry.slot() < layout.code().maximumLocals(),
                    entry,
                    "Invalid annotated local variable slot");
        }
    }

    private static <N extends JavaBytecodeNode> void bind(
            NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> rules,
            NodeKind<N> kind,
            BiConsumer<N, JvmValidationContext> validation) {
        rules.bind(
                kind,
                JavaAnnotationStructureValidation.class.getName(),
                (node, context) -> {
                    validation.accept(node, context);
                    return JvmValidationOutcome.VALID;
                });
    }
}
