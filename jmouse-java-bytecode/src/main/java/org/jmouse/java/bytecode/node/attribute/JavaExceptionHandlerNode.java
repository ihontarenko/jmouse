package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaExceptionHandlerNode extends JavaBytecodeNode {
    public static final NodeKind<JavaExceptionHandlerNode> KIND =
            new NodeKind<>(JvmNodeNames.EXCEPTION_HANDLER_NODE, JavaExceptionHandlerNode.class);

    public static final NodeSchema<JavaExceptionHandlerNode> SCHEMA = NodeSchema.of(KIND);
    private final int startPc;
    private final int endPc;
    private final int handlerPc;
    private final int catchTypeIndex;

    public JavaExceptionHandlerNode(
            BytesSource source,
            BytecodeSpan span,
            int startPc,
            int endPc,
            int handlerPc,
            int catchTypeIndex) {
        super(SCHEMA, source, span);
        this.startPc = startPc;
        this.endPc = endPc;
        this.handlerPc = handlerPc;
        this.catchTypeIndex = catchTypeIndex;
    }

    public int startPc() {
        return startPc;
    }

    public int endPc() {
        return endPc;
    }

    public int handlerPc() {
        return handlerPc;
    }

    public int catchTypeIndex() {
        return catchTypeIndex;
    }
}
