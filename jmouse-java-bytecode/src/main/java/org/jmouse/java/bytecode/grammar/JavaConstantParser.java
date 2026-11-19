package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaClassConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaDoubleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaDynamicConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaFieldReferenceConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaFloatConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaIntegerConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaInterfaceMethodReferenceConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaInvokeDynamicConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaLongConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaMethodHandleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaMethodReferenceConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaMethodTypeConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaModuleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaNameAndTypeConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaPackageConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaStringConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaUtf8ConstantNode;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;
import org.jmouse.java.bytecode.vocabulary.JvmReferenceKind;

public final class JavaConstantParser implements ParseRule<JavaConstantInput, JavaConstantNode> {
    @Override
    public JavaConstantNode parse(JavaConstantInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        return switch (JvmConstantTag.of(input.tag())) {
            case UTF8 -> {
                String value = JvmModifiedUtf8.read(bytes);
                yield new JavaUtf8ConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        value);
            }
            case INTEGER -> {
                int value = bytes.signed32();
                yield new JavaIntegerConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        value);
            }
            case FLOAT -> {
                int bits = bytes.signed32();
                yield new JavaFloatConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        bits);
            }
            case LONG -> {
                long value = bytes.signed64();
                yield new JavaLongConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        value);
            }
            case DOUBLE -> {
                long bits = bytes.signed64();
                yield new JavaDoubleConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        bits);
            }
            case CLASS -> {
                int nameIndex = bytes.unsigned16();
                yield new JavaClassConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        nameIndex);
            }
            case STRING -> {
                int stringIndex = bytes.unsigned16();
                yield new JavaStringConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        stringIndex);
            }
            case FIELD_REFERENCE -> {
                int classIndex = bytes.unsigned16();
                int nameAndTypeIndex = bytes.unsigned16();
                yield new JavaFieldReferenceConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        classIndex,
                        nameAndTypeIndex);
            }
            case METHOD_REFERENCE -> {
                int classIndex = bytes.unsigned16();
                int nameAndTypeIndex = bytes.unsigned16();
                yield new JavaMethodReferenceConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        classIndex,
                        nameAndTypeIndex);
            }
            case INTERFACE_METHOD_REFERENCE -> {
                int classIndex = bytes.unsigned16();
                int nameAndTypeIndex = bytes.unsigned16();
                yield new JavaInterfaceMethodReferenceConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        classIndex,
                        nameAndTypeIndex);
            }
            case NAME_AND_TYPE -> {
                int nameIndex = bytes.unsigned16();
                int descriptorIndex = bytes.unsigned16();
                yield new JavaNameAndTypeConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        nameIndex,
                        descriptorIndex);
            }
            case METHOD_HANDLE -> {
                JvmReferenceKind kind = JvmReferenceKind.of(bytes.unsigned8());
                int referenceIndex = bytes.unsigned16();
                yield new JavaMethodHandleConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        kind,
                        referenceIndex);
            }
            case METHOD_TYPE -> {
                int descriptorIndex = bytes.unsigned16();
                yield new JavaMethodTypeConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        descriptorIndex);
            }
            case DYNAMIC -> {
                int bootstrapMethodIndex = bytes.unsigned16();
                int nameAndTypeIndex = bytes.unsigned16();
                yield new JavaDynamicConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        bootstrapMethodIndex,
                        nameAndTypeIndex);
            }
            case INVOKE_DYNAMIC -> {
                int bootstrapMethodIndex = bytes.unsigned16();
                int nameAndTypeIndex = bytes.unsigned16();
                yield new JavaInvokeDynamicConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        bootstrapMethodIndex,
                        nameAndTypeIndex);
            }
            case MODULE -> {
                int nameIndex = bytes.unsigned16();
                yield new JavaModuleConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        nameIndex);
            }
            case PACKAGE -> {
                int nameIndex = bytes.unsigned16();
                yield new JavaPackageConstantNode(
                        bytes.source(),
                        new BytecodeSpan(input.start(), bytes.position()),
                        input.index(),
                        nameIndex);
            }
        };
    }
}
