package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationElementNode;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationValueNode;

import java.util.ArrayList;
import java.util.List;

public final class JavaAnnotationParser
        implements ParseRule<JavaAnnotationInput, JavaAnnotationNode> {
    @Override
    public JavaAnnotationNode parse(JavaAnnotationInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        long start = bytes.position();
        int typeIndex = bytes.unsigned16();
        input.pool().utf8(typeIndex);
        int count = bytes.unsigned16();
        List<JavaAnnotationElementNode> elements = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long elementStart = bytes.position();
            int name = bytes.unsigned16();
            input.pool().utf8(name);
            JavaAnnotationValueNode value =
                    session.parse(JavaBytecodeRoles.ANNOTATION_VALUE, input);
            elements.add(
                    new JavaAnnotationElementNode(
                            bytes.source(),
                            new BytecodeSpan(elementStart, bytes.position()),
                            name,
                            value));
        }
        return new JavaAnnotationNode(
                bytes.source(), new BytecodeSpan(start, bytes.position()), typeIndex, elements);
    }
}
