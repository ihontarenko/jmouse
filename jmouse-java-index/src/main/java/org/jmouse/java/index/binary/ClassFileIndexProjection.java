package org.jmouse.java.index.binary;

import org.jmouse.java.bytecode.node.JavaClassFileNode;

@FunctionalInterface
public interface ClassFileIndexProjection {
    ClassFileIndexer.IndexedClass project(String source, JavaClassFileNode classFile);
}
