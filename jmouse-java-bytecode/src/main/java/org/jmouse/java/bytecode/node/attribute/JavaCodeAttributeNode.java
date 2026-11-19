package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.JavaCodeCoverage;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;
import java.util.Objects;

public final class JavaCodeAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaCodeAttributeNode> KIND =
            new NodeKind<>(JvmNodeNames.CODE_ATTRIBUTE_NODE, JavaCodeAttributeNode.class);
    public static final ChildRole<JavaInstructionNode> INSTRUCTIONS =
            ChildRole.list(JavaBytecodeNames.INSTRUCTIONS, JavaInstructionNode.class);
    public static final ChildRole<JavaExceptionHandlerNode> HANDLERS =
            ChildRole.list(JavaBytecodeNames.HANDLERS, JavaExceptionHandlerNode.class);
    public static final ChildRole<JavaAttributeNode> ATTRIBUTES =
            ChildRole.list(JavaBytecodeNames.ATTRIBUTES, JavaAttributeNode.class);
    public static final NodeSchema<JavaCodeAttributeNode> SCHEMA =
            NodeSchema.of(KIND, INSTRUCTIONS, HANDLERS, ATTRIBUTES);
    private final int maximumStack;
    private final int maximumLocals;
    private final BytecodeSpan code;
    private final JavaCodeCoverage coverage;

    public JavaCodeAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int maximumStack,
            int maximumLocals,
            BytecodeSpan code,
            JavaCodeCoverage coverage,
            List<JavaInstructionNode> instructions,
            List<JavaExceptionHandlerNode> handlers,
            List<JavaAttributeNode> attributes) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.maximumStack = maximumStack;
        this.maximumLocals = maximumLocals;
        this.code = code;
        this.coverage = Objects.requireNonNull(coverage, "coverage");
        instructions.forEach(instruction -> addChild(INSTRUCTIONS, instruction));
        handlers.forEach(handler -> addChild(HANDLERS, handler));
        attributes.forEach(attribute -> addChild(ATTRIBUTES, attribute));
    }

    public int maximumStack() {
        return maximumStack;
    }

    public int maximumLocals() {
        return maximumLocals;
    }

    public BytecodeSpan code() {
        return code;
    }

    public JavaCodeCoverage coverage() {
        return coverage;
    }

    @Override
    protected void validateStructure() {
        if (coverage == JavaCodeCoverage.OMITTED && !instructions().isEmpty()
                || coverage == JavaCodeCoverage.COMPLETE && instructions().isEmpty()) {
            throw new IllegalStateException(
                    "Code instructions do not match their declared coverage");
        }
    }

    public List<JavaInstructionNode> instructions() {
        return children(INSTRUCTIONS);
    }

    public List<JavaExceptionHandlerNode> handlers() {
        return children(HANDLERS);
    }

    public List<JavaAttributeNode> attributes() {
        return children(ATTRIBUTES);
    }
}
