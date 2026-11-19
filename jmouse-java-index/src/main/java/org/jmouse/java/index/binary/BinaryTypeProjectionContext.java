package org.jmouse.java.index.binary;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.java.bytecode.signature.JvmTextNode;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;

public record BinaryTypeProjectionContext(
        BinaryProjectionContext projection,
        SymbolId owner,
        SymbolId scopeType,
        TypeReferenceRole role) {
    public SourceWriter writer() {
        return projection.writer();
    }

    public void write(JvmTextNode node) {
        projection.step();
        projection.configuration().types().apply(node, this);
    }

    public void write(JvmTextNode node, TypeReferenceRole replacement) {
        new BinaryTypeProjectionContext(projection, owner, scopeType, replacement).write(node);
    }

    public String qualifiedName(String internalName) {
        return projection.qualifiedName(internalName);
    }

    public void reference(String name, int start, int end) {
        projection.reference(name, role, new BinaryTextRange(start, end), owner, scopeType);
    }
}
