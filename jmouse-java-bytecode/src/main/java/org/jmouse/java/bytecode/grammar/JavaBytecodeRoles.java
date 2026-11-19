package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationValueNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaVerificationTypeNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;

public final class JavaBytecodeRoles {
    public static final ParseRole<JavaStackMapInput, JavaStackMapFrameNode> STACK_MAP_FRAME =
            new ParseRole<>(
                    JavaBytecodeNames.STACK_MAP_FRAME_ROLE,
                    JavaStackMapInput.class,
                    JavaStackMapFrameNode.class);
    public static final ParseRole<JavaStackMapInput, JavaVerificationTypeNode> VERIFICATION_TYPE =
            new ParseRole<>(
                    JavaBytecodeNames.VERIFICATION_TYPE_ROLE,
                    JavaStackMapInput.class,
                    JavaVerificationTypeNode.class);
    public static final ParseRole<JavaAnnotationInput, JavaAnnotationNode> ANNOTATION =
            new ParseRole<>(
                    JavaBytecodeNames.ANNOTATION_ROLE,
                    JavaAnnotationInput.class,
                    JavaAnnotationNode.class);
    public static final ParseRole<JavaAnnotationInput, JavaAnnotationValueNode> ANNOTATION_VALUE =
            new ParseRole<>(
                    JavaBytecodeNames.ANNOTATION_VALUE_ROLE,
                    JavaAnnotationInput.class,
                    JavaAnnotationValueNode.class);
    public static final ParseRole<JavaAnnotationInput, JavaTypeAnnotationNode> TYPE_ANNOTATION =
            new ParseRole<>(
                    JavaBytecodeNames.TYPE_ANNOTATION_ROLE,
                    JavaAnnotationInput.class,
                    JavaTypeAnnotationNode.class);
    public static final ParseRole<JavaInstructionInput, JavaInstructionNode> INSTRUCTION =
            new ParseRole<>(
                    JavaBytecodeNames.INSTRUCTION_ROLE,
                    JavaInstructionInput.class,
                    JavaInstructionNode.class);

    private JavaBytecodeRoles() {}
}
