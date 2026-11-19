package org.jmouse.java.index.binary;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.bytecode.signature.*;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.model.TypeReferenceRole;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Each generic syntax family has an addressable writer; no grammar is reconstructed from strings.
 */
public final class BinaryTypeProjections {
    private BinaryTypeProjections() {}

    public static NodeOperationRegistry<BinaryTypeProjectionContext, BinaryProjectionOutcome>
            standard() {
        NodeOperationRegistry<BinaryTypeProjectionContext, BinaryProjectionOutcome> operations =
                new NodeOperationRegistry<>(BinaryProjectionNames.TYPE);
        bind(
                operations,
                JvmScalarDescriptorNode.KIND,
                (node, context) ->
                        context.writer().word(((JavaType) node.descriptor()).displayName()));
        bind(
                operations,
                JvmClassDescriptorNode.KIND,
                (node, context) ->
                        named(context.qualifiedName(node.descriptor().internalName()), context));
        bind(
                operations,
                JvmArrayDescriptorNode.KIND,
                (node, context) -> {
                    context.write(node.element());
                    context.writer()
                            .join(BinaryProjectionNames.ARRAY_SUFFIX.repeat(node.dimensions()));
                });
        bind(
                operations,
                JvmArraySignatureNode.KIND,
                (node, context) -> {
                    context.write(node.element());
                    context.writer()
                            .join(BinaryProjectionNames.ARRAY_SUFFIX.repeat(node.dimensions()));
                });
        bind(
                operations,
                JvmTypeVariableSignatureNode.KIND,
                (node, context) -> named(node.name(), context));
        bind(operations, JvmClassTypeSignatureNode.KIND, BinaryTypeProjections::classType);
        bind(
                operations,
                JvmClassTypeSegmentNode.KIND,
                (node, context) -> {
                    context.writer().word(node.name());
                    arguments(node.arguments(), context);
                });
        bind(operations, JvmTypeArgumentNode.KIND, BinaryTypeProjections::argument);
        bind(operations, JvmTypeParameterNode.KIND, BinaryTypeProjections::parameter);
        return operations;
    }

    private static void named(String name, BinaryTypeProjectionContext context) {
        context.writer().word(name);
        context.reference(
                name, context.writer().length() - name.length(), context.writer().length());
    }

    private static void classType(
            JvmClassTypeSignatureNode node, BinaryTypeProjectionContext context) {
        JvmClassTypeSegmentNode first = node.segments().getFirst();
        String name = context.qualifiedName(node.packageSpecifier() + first.name());
        context.writer().word(name);
        int start = context.writer().length() - name.length();
        context.reference(name, start, context.writer().length());
        arguments(first.arguments(), context);
        String internalName = node.packageSpecifier() + first.name();
        for (int index = 1; index < node.segments().size(); index++) {
            context.writer().opening(JavaToken.DOT.spelling());
            int segmentStart = context.writer().length();
            context.write(node.segments().get(index));
            internalName +=
                    BinaryProjectionNames.BINARY_NESTED_SEPARATOR
                            + node.segments().get(index).name();
            context.reference(
                    context.qualifiedName(internalName),
                    segmentStart,
                    segmentStart + node.segments().get(index).name().length());
        }
    }

    private static void arguments(
            List<JvmTypeArgumentNode> arguments, BinaryTypeProjectionContext context) {
        if (arguments.isEmpty()) {
            return;
        }
        context.writer().opening(JavaToken.LESS.spelling());
        for (int index = 0; index < arguments.size(); index++) {
            if (index > 0) {
                context.writer().join(JavaToken.COMMA.spelling());
            }
            context.write(arguments.get(index), TypeReferenceRole.TYPE_ARGUMENT);
        }
        context.writer().join(JavaToken.GREATER.spelling());
    }

    private static void argument(JvmTypeArgumentNode node, BinaryTypeProjectionContext context) {
        switch (node.variance()) {
            case UNBOUNDED -> context.writer().word(JavaToken.QUESTION.spelling());
            case EXACT ->
                    context.write(node.bound().orElseThrow(), TypeReferenceRole.TYPE_ARGUMENT);
            case EXTENDS, SUPER -> {
                context.writer()
                        .word(JavaToken.QUESTION.spelling())
                        .word(
                                node.variance() == JvmTypeArgumentVariance.EXTENDS
                                        ? JavaKeyword.EXTENDS.spelling()
                                        : JavaKeyword.SUPER.spelling());
                context.write(node.bound().orElseThrow(), TypeReferenceRole.TYPE_ARGUMENT);
            }
        }
    }

    private static void parameter(JvmTypeParameterNode node, BinaryTypeProjectionContext context) {
        context.writer().word(node.name());
        List<JvmReferenceTypeSignatureNode> bounds = new ArrayList<>();
        node.classBound().ifPresent(bounds::add);
        bounds.addAll(node.interfaceBounds());
        if (!bounds.isEmpty()) {
            context.writer().word(JavaKeyword.EXTENDS.spelling());
            for (int index = 0; index < bounds.size(); index++) {
                if (index > 0) {
                    context.writer().word(JavaToken.AMPERSAND.spelling());
                }
                context.write(bounds.get(index), TypeReferenceRole.TYPE_PARAMETER_BOUND);
            }
        }
    }

    private static <N extends JvmTextNode> void bind(
            NodeOperationRegistry<BinaryTypeProjectionContext, BinaryProjectionOutcome> operations,
            NodeKind<N> kind,
            BiConsumer<N, BinaryTypeProjectionContext> writer) {
        operations.bind(
                kind,
                BinaryTypeProjections.class.getName(),
                (node, context) -> {
                    writer.accept(node, context);
                    return BinaryProjectionOutcome.WRITTEN;
                });
    }
}
