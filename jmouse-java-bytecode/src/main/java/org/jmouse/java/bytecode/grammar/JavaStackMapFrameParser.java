package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.attribute.JavaAppendFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaChopFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaFullFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaSameFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaSameLocalsOneStackItemFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaVerificationTypeNode;
import org.jmouse.java.bytecode.vocabulary.JvmStackMapFrameKind;

import java.util.ArrayList;
import java.util.List;

public final class JavaStackMapFrameParser
        implements ParseRule<JavaStackMapInput, JavaStackMapFrameNode> {
    @Override
    public JavaStackMapFrameNode parse(JavaStackMapInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        long start = bytes.position();
        int type = bytes.unsigned8();
        JvmStackMapFrameKind kind = JvmStackMapFrameKind.of(type);
        return switch (kind) {
            case SAME_FRAME ->
                    new JavaSameFrameNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            type,
                            type - kind.minimum());
            case SAME_FRAME_EXTENDED -> {
                int delta = bytes.unsigned16();
                yield new JavaSameFrameNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), type, delta);
            }
            case SAME_LOCALS_ONE_STACK_ITEM_FRAME, SAME_LOCALS_ONE_STACK_ITEM_FRAME_EXTENDED -> {
                int delta =
                        kind == JvmStackMapFrameKind.SAME_LOCALS_ONE_STACK_ITEM_FRAME
                                ? type - kind.minimum()
                                : bytes.unsigned16();
                JavaVerificationTypeNode value =
                        session.parse(JavaBytecodeRoles.VERIFICATION_TYPE, input);
                yield new JavaSameLocalsOneStackItemFrameNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        type,
                        delta,
                        value);
            }
            case CHOP_FRAME -> {
                int delta = bytes.unsigned16();
                int chopped = JvmStackMapFrameKind.SAME_FRAME_EXTENDED.minimum() - type;
                yield new JavaChopFrameNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        type,
                        delta,
                        chopped);
            }
            case APPEND_FRAME -> {
                int delta = bytes.unsigned16();
                List<JavaVerificationTypeNode> locals =
                        verificationTypes(
                                input,
                                session,
                                type - JvmStackMapFrameKind.SAME_FRAME_EXTENDED.minimum());
                yield new JavaAppendFrameNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        type,
                        delta,
                        locals);
            }
            case FULL_FRAME -> {
                int delta = bytes.unsigned16();
                List<JavaVerificationTypeNode> locals =
                        verificationTypes(input, session, bytes.unsigned16());
                List<JavaVerificationTypeNode> stack =
                        verificationTypes(input, session, bytes.unsigned16());
                yield new JavaFullFrameNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        type,
                        delta,
                        locals,
                        stack);
            }
            case RESERVED -> throw bytes.failure("Reserved stack-map frame type");
        };
    }

    private static List<JavaVerificationTypeNode> verificationTypes(
            JavaStackMapInput input, ParseSession session, int count) {
        List<JavaVerificationTypeNode> types = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            types.add(session.parse(JavaBytecodeRoles.VERIFICATION_TYPE, input));
        }
        return List.copyOf(types);
    }
}
