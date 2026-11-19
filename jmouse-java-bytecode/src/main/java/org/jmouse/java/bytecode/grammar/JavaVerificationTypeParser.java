package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.attribute.JavaDoubleVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaFloatVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaIntegerVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLongVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaNullVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaObjectVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaTopVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaUninitializedThisVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaUninitializedVerificationTypeNode;
import org.jmouse.java.bytecode.node.attribute.JavaVerificationTypeNode;
import org.jmouse.java.bytecode.node.constant.JavaClassConstantNode;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaVerificationTypeParser
        implements ParseRule<JavaStackMapInput, JavaVerificationTypeNode> {
    @Override
    public JavaVerificationTypeNode parse(JavaStackMapInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        long start = bytes.position();
        JvmVerificationTypeTag tag = JvmVerificationTypeTag.of(bytes.unsigned8());
        return switch (tag) {
            case TOP ->
                    new JavaTopVerificationTypeNode(
                            bytes.source(), new BytecodeSpan(start, bytes.position()));
            case INTEGER ->
                    new JavaIntegerVerificationTypeNode(
                            bytes.source(), new BytecodeSpan(start, bytes.position()));
            case FLOAT ->
                    new JavaFloatVerificationTypeNode(
                            bytes.source(), new BytecodeSpan(start, bytes.position()));
            case DOUBLE ->
                    new JavaDoubleVerificationTypeNode(
                            bytes.source(), new BytecodeSpan(start, bytes.position()));
            case LONG ->
                    new JavaLongVerificationTypeNode(
                            bytes.source(), new BytecodeSpan(start, bytes.position()));
            case NULL ->
                    new JavaNullVerificationTypeNode(
                            bytes.source(), new BytecodeSpan(start, bytes.position()));
            case UNINITIALIZED_THIS ->
                    new JavaUninitializedThisVerificationTypeNode(
                            bytes.source(), new BytecodeSpan(start, bytes.position()));
            case OBJECT -> {
                int index = bytes.unsigned16();
                input.pool().require(index, JavaClassConstantNode.class);
                yield new JavaObjectVerificationTypeNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), index);
            }
            case UNINITIALIZED -> {
                int offset = bytes.unsigned16();
                yield new JavaUninitializedVerificationTypeNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), offset);
            }
        };
    }
}
