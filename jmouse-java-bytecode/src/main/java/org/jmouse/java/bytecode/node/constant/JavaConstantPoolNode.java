package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JavaConstantPoolNode extends JavaBytecodeNode {
    public static final NodeKind<JavaConstantPoolNode> KIND =
            new NodeKind<>(JavaBytecodeNames.CONSTANT_ROLE, JavaConstantPoolNode.class);
    public static final ChildRole<JavaConstantNode> CONSTANTS =
            ChildRole.list(JavaBytecodeNames.CONSTANTS, JavaConstantNode.class);
    public static final NodeSchema<JavaConstantPoolNode> SCHEMA = NodeSchema.of(KIND, CONSTANTS);
    private final int slotCount;
    private final Map<Integer, JavaConstantNode> indices;

    public JavaConstantPoolNode(
            BytesSource source, BytecodeSpan span, int slotCount, List<JavaConstantNode> entries) {
        super(SCHEMA, source, span);
        this.slotCount = slotCount;
        Map<Integer, JavaConstantNode> indices = new HashMap<>();
        for (JavaConstantNode entry : entries) {
            if (entry.index() >= slotCount || indices.putIfAbsent(entry.index(), entry) != null) {
                throw new IllegalArgumentException("Invalid constant pool slot");
            }
            addChild(CONSTANTS, entry);
        }
        this.indices = Map.copyOf(indices);
    }

    public int slotCount() {
        return slotCount;
    }

    public List<JavaConstantNode> entries() {
        return children(CONSTANTS);
    }

    public JavaConstantNode entry(int index) {
        JavaConstantNode entry = indices.get(index);
        if (entry == null) {
            throw new IllegalArgumentException("Invalid or unusable constant pool index: " + index);
        }
        return entry;
    }

    public <T extends JavaConstantNode> T require(int index, Class<T> type) {
        JavaConstantNode entry = entry(index);
        if (!type.isInstance(entry)) {
            throw new IllegalArgumentException(
                    "Wrong constant pool tag at " + index + ": " + entry.tag());
        }
        return type.cast(entry);
    }

    public String utf8(int index) {
        return require(index, JavaUtf8ConstantNode.class).value();
    }

    public String className(int index) {
        return utf8(require(index, JavaClassConstantNode.class).nameIndex());
    }
}
