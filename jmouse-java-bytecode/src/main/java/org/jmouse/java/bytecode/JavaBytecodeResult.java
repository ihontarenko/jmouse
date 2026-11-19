package org.jmouse.java.bytecode;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.model.version.VersionAdmission;

import java.util.Objects;

public record JavaBytecodeResult(
        JavaClassFileNode root, JavaBytecodeMode mode, VersionAdmission admission) {
    public JavaBytecodeResult {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(admission, "admission");
        if (root.lifecycle() != NodeLifecycle.FROZEN || root.getParent() != null) {
            throw new IllegalArgumentException("A binary result requires a frozen root");
        }
    }

    public BytesSource source() {
        return root.source();
    }

    public boolean isComplete() {
        return mode == JavaBytecodeMode.COMPLETE
                && admission == VersionAdmission.ACCEPTED
                && root.methods().stream()
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .allMatch(code -> code.coverage() == JavaCodeCoverage.COMPLETE);
    }

    public JavaClassFileNode requireComplete() {
        if (!isComplete()) {
            throw new IllegalStateException("This operation requires complete admitted bytecode");
        }
        return root;
    }
}
