package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaCatchTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaEmptyTypeAnnotationTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaFormalParameterTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTargetEntryNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaOffsetTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaSupertypeTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaThrowsTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeAnnotationTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeArgumentTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeParameterBoundTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeParameterTargetNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypePathEntryNode;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;
import org.jmouse.java.bytecode.vocabulary.JvmTypePathKind;

import java.util.ArrayList;
import java.util.List;

public final class JavaTypeAnnotationParser
        implements ParseRule<JavaAnnotationInput, JavaTypeAnnotationNode> {
    @Override
    public JavaTypeAnnotationNode parse(JavaAnnotationInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        long start = bytes.position();
        JvmTypeAnnotationTarget target = JvmTypeAnnotationTarget.of(bytes.unsigned8());
        JavaTypeAnnotationTargetNode targetNode = target(bytes, target);
        int count = bytes.unsigned8();
        List<JavaTypePathEntryNode> path = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long entryStart = bytes.position();
            JvmTypePathKind kind = JvmTypePathKind.of(bytes.unsigned8());
            int argument = bytes.unsigned8();
            if (kind != JvmTypePathKind.TYPE_ARGUMENT && argument != 0) {
                throw bytes.failure(
                        "A non-argument type path entry must have a zero argument index");
            }
            path.add(
                    new JavaTypePathEntryNode(
                            bytes.source(),
                            new BytecodeSpan(entryStart, bytes.position()),
                            kind,
                            argument));
        }
        JavaAnnotationNode annotation = session.parse(JavaBytecodeRoles.ANNOTATION, input);
        return new JavaTypeAnnotationNode(
                bytes.source(),
                new BytecodeSpan(start, bytes.position()),
                targetNode,
                path,
                annotation);
    }

    private static JavaTypeAnnotationTargetNode target(
            BytesCursor bytes, JvmTypeAnnotationTarget target) {
        long start = bytes.position();
        return switch (target) {
            case CLASS_TYPE_PARAMETER, METHOD_TYPE_PARAMETER -> {
                int parameterIndex = bytes.unsigned8();
                yield new JavaTypeParameterTargetNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        target,
                        parameterIndex);
            }
            case SUPERTYPE -> {
                int supertypeIndex = bytes.unsigned16();
                yield new JavaSupertypeTargetNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        target,
                        supertypeIndex);
            }
            case CLASS_TYPE_PARAMETER_BOUND, METHOD_TYPE_PARAMETER_BOUND -> {
                int parameterIndex = bytes.unsigned8();
                int boundIndex = bytes.unsigned8();
                yield new JavaTypeParameterBoundTargetNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        target,
                        parameterIndex,
                        boundIndex);
            }
            case FIELD, METHOD_RETURN, METHOD_RECEIVER -> {
                yield new JavaEmptyTypeAnnotationTargetNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), target);
            }
            case METHOD_FORMAL_PARAMETER -> {
                int parameterIndex = bytes.unsigned8();
                yield new JavaFormalParameterTargetNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        target,
                        parameterIndex);
            }
            case THROWS -> {
                int throwsIndex = bytes.unsigned16();
                yield new JavaThrowsTargetNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        target,
                        throwsIndex);
            }
            case EXCEPTION_PARAMETER -> {
                int exceptionIndex = bytes.unsigned16();
                yield new JavaCatchTargetNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        target,
                        exceptionIndex);
            }
            case INSTANCEOF, NEW, CONSTRUCTOR_REFERENCE, METHOD_REFERENCE -> {
                int offset = bytes.unsigned16();
                yield new JavaOffsetTargetNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), target, offset);
            }
            case CAST,
                    CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
                    METHOD_INVOCATION_TYPE_ARGUMENT,
                    CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
                    METHOD_REFERENCE_TYPE_ARGUMENT -> {
                int offset = bytes.unsigned16();
                int argumentIndex = bytes.unsigned8();
                yield new JavaTypeArgumentTargetNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        target,
                        offset,
                        argumentIndex);
            }
            case LOCAL_VARIABLE, RESOURCE_VARIABLE -> {
                int count = bytes.unsigned16();
                List<JavaLocalVariableTargetEntryNode> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long entryStart = bytes.position();
                    int offset = bytes.unsigned16();
                    int length = bytes.unsigned16();
                    int slot = bytes.unsigned16();
                    entries.add(
                            new JavaLocalVariableTargetEntryNode(
                                    bytes.source(),
                                    new BytecodeSpan(entryStart, bytes.position()),
                                    offset,
                                    length,
                                    slot));
                }
                yield new JavaLocalVariableTargetNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), target, entries);
            }
        };
    }
}
