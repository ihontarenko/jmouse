package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationValueNode;
import org.jmouse.java.bytecode.node.attribute.JavaArrayAnnotationValueNode;
import org.jmouse.java.bytecode.node.attribute.JavaClassAnnotationValueNode;
import org.jmouse.java.bytecode.node.attribute.JavaConstantAnnotationValueNode;
import org.jmouse.java.bytecode.node.attribute.JavaEnumAnnotationValueNode;
import org.jmouse.java.bytecode.node.attribute.JavaNestedAnnotationValueNode;
import org.jmouse.java.bytecode.node.constant.JavaDoubleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaFloatConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaIntegerConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaLongConstantNode;
import org.jmouse.java.bytecode.vocabulary.JvmAnnotationValueTag;

import java.util.ArrayList;
import java.util.List;

public final class JavaAnnotationValueParser
        implements ParseRule<JavaAnnotationInput, JavaAnnotationValueNode> {
    @Override
    public JavaAnnotationValueNode parse(JavaAnnotationInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        long start = bytes.position();
        JvmAnnotationValueTag tag = JvmAnnotationValueTag.of(bytes.unsigned8());
        return switch (tag) {
            case BYTE, CHAR, INT, SHORT, BOOLEAN, FLOAT, LONG, DOUBLE, STRING -> {
                int constant = bytes.unsigned16();
                switch (tag) {
                    case BYTE, CHAR, INT, SHORT, BOOLEAN ->
                            input.pool().require(constant, JavaIntegerConstantNode.class);
                    case FLOAT -> input.pool().require(constant, JavaFloatConstantNode.class);
                    case DOUBLE -> input.pool().require(constant, JavaDoubleConstantNode.class);
                    case LONG -> input.pool().require(constant, JavaLongConstantNode.class);
                    case STRING -> input.pool().utf8(constant);
                    default -> throw bytes.failure("Invalid constant annotation tag");
                }
                yield new JavaConstantAnnotationValueNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), tag, constant);
            }
            case ENUM -> {
                int type = bytes.unsigned16();
                int constant = bytes.unsigned16();
                input.pool().utf8(type);
                input.pool().utf8(constant);
                yield new JavaEnumAnnotationValueNode(
                        bytes.source(),
                        new BytecodeSpan(start, bytes.position()),
                        tag,
                        type,
                        constant);
            }
            case CLASS -> {
                int type = bytes.unsigned16();
                input.pool().utf8(type);
                yield new JavaClassAnnotationValueNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), tag, type);
            }
            case ANNOTATION -> {
                JavaAnnotationNode annotation = session.parse(JavaBytecodeRoles.ANNOTATION, input);
                yield new JavaNestedAnnotationValueNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), tag, annotation);
            }
            case ARRAY -> {
                int count = bytes.unsigned16();
                List<JavaAnnotationValueNode> values = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    values.add(session.parse(JavaBytecodeRoles.ANNOTATION_VALUE, input));
                }
                yield new JavaArrayAnnotationValueNode(
                        bytes.source(), new BytecodeSpan(start, bytes.position()), tag, values);
            }
        };
    }
}
