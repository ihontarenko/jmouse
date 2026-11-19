package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.instruction.JavaBranchInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaConstantInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaImmediateInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaIncrementInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInvokeDynamicInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInvokeInterfaceInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaLocalInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaLookupSwitchInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaMultiArrayInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaNoOperandInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaPrimitiveArrayInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaTableSwitchInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JvmInstructionEncoding;
import org.jmouse.java.bytecode.node.instruction.JvmLookupSwitchPair;
import org.jmouse.java.bytecode.vocabulary.JvmFormatLimits;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;
import org.jmouse.java.bytecode.vocabulary.JvmOpcodeClassification;
import org.jmouse.java.bytecode.vocabulary.JvmOperandEncoding;
import org.jmouse.java.bytecode.vocabulary.JvmPrimitiveArrayType;

import java.util.ArrayList;
import java.util.List;

public final class JavaInstructionParser
        implements ParseRule<JavaInstructionInput, JavaInstructionNode> {
    @Override
    public JavaInstructionNode parse(JavaInstructionInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        long start = bytes.position();
        int offset = Math.toIntExact(start - input.codeStart());
        JvmOpcode opcode = JvmOpcode.of(bytes.unsigned8());
        JvmInstructionEncoding encoding = JvmInstructionEncoding.ORDINARY;
        if (opcode == JvmOpcode.WIDE) {
            encoding = JvmInstructionEncoding.WIDE;
            opcode = JvmOpcode.of(bytes.unsigned8());
            if (opcode.operands() != JvmOperandEncoding.LOCAL && opcode != JvmOpcode.IINC) {
                throw bytes.failure("WIDE cannot modify " + opcode);
            }
        }
        if (opcode.classification() != JvmOpcodeClassification.STANDARD) {
            throw bytes.failure("Reserved or unassigned opcode in Code: " + opcode);
        }
        return switch (opcode.operands()) {
            case NONE ->
                    new JavaNoOperandInstructionNode(
                            bytes.source(), span(bytes, start), opcode, offset, encoding);
            case SIGNED_BYTE, SIGNED_SHORT -> {
                int value =
                        opcode.operands() == JvmOperandEncoding.SIGNED_BYTE
                                ? bytes.signed8()
                                : bytes.signed16();
                yield new JavaImmediateInstructionNode(
                        bytes.source(), span(bytes, start), opcode, offset, encoding, value);
            }
            case CONSTANT_BYTE, CONSTANT_SHORT -> {
                int index =
                        opcode.operands() == JvmOperandEncoding.CONSTANT_BYTE
                                ? bytes.unsigned8()
                                : bytes.unsigned16();
                yield new JavaConstantInstructionNode(
                        bytes.source(), span(bytes, start), opcode, offset, encoding, index);
            }
            case LOCAL -> {
                int slot =
                        encoding == JvmInstructionEncoding.WIDE
                                ? bytes.unsigned16()
                                : bytes.unsigned8();
                yield new JavaLocalInstructionNode(
                        bytes.source(), span(bytes, start), opcode, offset, encoding, slot);
            }
            case INCREMENT -> {
                int slot =
                        encoding == JvmInstructionEncoding.WIDE
                                ? bytes.unsigned16()
                                : bytes.unsigned8();
                int increment =
                        encoding == JvmInstructionEncoding.WIDE
                                ? bytes.signed16()
                                : bytes.signed8();
                yield new JavaIncrementInstructionNode(
                        bytes.source(),
                        span(bytes, start),
                        opcode,
                        offset,
                        encoding,
                        slot,
                        increment);
            }
            case BRANCH_SHORT, BRANCH_INT -> {
                int displacement =
                        opcode.operands() == JvmOperandEncoding.BRANCH_SHORT
                                ? bytes.signed16()
                                : bytes.signed32();
                yield new JavaBranchInstructionNode(
                        bytes.source(), span(bytes, start), opcode, offset, encoding, displacement);
            }
            case INTERFACE_CALL -> {
                int index = bytes.unsigned16();
                int count = bytes.unsigned8();
                if (bytes.unsigned8() != 0 || count == 0) {
                    throw bytes.failure("Invalid invokeinterface count or reserved byte");
                }
                yield new JavaInvokeInterfaceInstructionNode(
                        bytes.source(), span(bytes, start), opcode, offset, encoding, index, count);
            }
            case DYNAMIC_CALL -> {
                int index = bytes.unsigned16();
                if (bytes.unsigned16() != 0) {
                    throw bytes.failure("Non-zero invokedynamic reserved bytes");
                }
                yield new JavaInvokeDynamicInstructionNode(
                        bytes.source(), span(bytes, start), opcode, offset, encoding, index);
            }
            case PRIMITIVE_ARRAY -> {
                JvmPrimitiveArrayType type = JvmPrimitiveArrayType.of(bytes.unsigned8());
                yield new JavaPrimitiveArrayInstructionNode(
                        bytes.source(), span(bytes, start), opcode, offset, encoding, type);
            }
            case MULTI_ARRAY -> {
                int index = bytes.unsigned16();
                int dimensions = bytes.unsigned8();
                if (dimensions == 0) {
                    throw bytes.failure("A multianewarray instruction requires dimensions");
                }
                yield new JavaMultiArrayInstructionNode(
                        bytes.source(),
                        span(bytes, start),
                        opcode,
                        offset,
                        encoding,
                        index,
                        dimensions);
            }
            case TABLE_SWITCH -> {
                int padding = padding(bytes, input.codeStart());
                int defaultDisplacement = bytes.signed32();
                int low = bytes.signed32();
                int high = bytes.signed32();
                long count = (long) high - low + 1;
                if (count <= 0 || count > bytes.remaining() / Integer.BYTES) {
                    throw bytes.failure("Invalid tableswitch bounds");
                }
                List<Integer> displacements = new ArrayList<>((int) count);
                for (int index = 0; index < count; index++) {
                    displacements.add(bytes.signed32());
                }
                yield new JavaTableSwitchInstructionNode(
                        bytes.source(),
                        span(bytes, start),
                        opcode,
                        offset,
                        encoding,
                        defaultDisplacement,
                        low,
                        high,
                        displacements,
                        padding);
            }
            case LOOKUP_SWITCH -> {
                int padding = padding(bytes, input.codeStart());
                int defaultDisplacement = bytes.signed32();
                int count = bytes.signed32();
                if (count < 0 || count > bytes.remaining() / (Integer.BYTES * 2)) {
                    throw bytes.failure("Invalid lookupswitch pair count");
                }
                List<JvmLookupSwitchPair> pairs = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    int match = bytes.signed32();
                    if (index > 0 && match <= pairs.getLast().match()) {
                        throw bytes.failure("Unsorted lookupswitch keys");
                    }
                    pairs.add(new JvmLookupSwitchPair(match, bytes.signed32()));
                }
                yield new JavaLookupSwitchInstructionNode(
                        bytes.source(),
                        span(bytes, start),
                        opcode,
                        offset,
                        encoding,
                        defaultDisplacement,
                        pairs,
                        padding);
            }
            case WIDE, INVALID -> throw bytes.failure("Invalid instruction encoding");
        };
    }

    private static BytecodeSpan span(BytesCursor bytes, long start) {
        return new BytecodeSpan(start, bytes.position());
    }

    private static int padding(BytesCursor bytes, long codeStart) {
        int count =
                Math.toIntExact(
                        (JvmFormatLimits.SWITCH_ALIGNMENT
                                        - (bytes.position() - codeStart)
                                                % JvmFormatLimits.SWITCH_ALIGNMENT)
                                % JvmFormatLimits.SWITCH_ALIGNMENT);
        for (int index = 0; index < count; index++) {
            if (bytes.unsigned8() != 0) {
                throw bytes.failure("Non-zero switch padding");
            }
        }
        return count;
    }
}
