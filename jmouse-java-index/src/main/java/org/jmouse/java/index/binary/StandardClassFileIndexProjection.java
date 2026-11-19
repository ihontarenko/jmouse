package org.jmouse.java.index.binary;

import org.jmouse.java.bytecode.node.JavaClassFileNode;

import java.util.Objects;

/** Direct classfile AST to declaration index and a readable API view. */
public final class StandardClassFileIndexProjection implements ClassFileIndexProjection {
    private final ClassFileProjectionConfiguration configuration;

    public StandardClassFileIndexProjection() {
        this(ClassFileProjectionConfiguration.standard());
    }

    public StandardClassFileIndexProjection(ClassFileProjectionConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public ClassFileIndexer.IndexedClass project(String source, JavaClassFileNode classFile) {
        return new BinaryDeclarationProjection(
                        new BinaryProjectionContext(source, classFile, configuration))
                .project();
    }
}
