package org.jmouse.java.parser.node;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.el.node.Node;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.RecordComponentDeclaration;
import org.jmouse.java.parser.model.TypeDeclaration;

/**
 * A type declaration. Its children are its record components, then its members in source order — nested
 * types are {@code TypeNode}s, everything else {@link MemberNode}s.
 *
 * <p>The node holds the type's own header; the {@link TypeDeclaration} record with its members is
 * projected from the children on every call to {@link #declaration()}, so the tree stays the one model.
 */
public final class TypeNode extends JavaNode implements DeclarationNode {

    private final TypeDeclaration header;

    /**
     * Creates the node.
     *
     * @param header the type's declaration; its record components and members are ignored — they come from the children
     */
    public TypeNode(TypeDeclaration header) {
        super(header.span());
        this.header = header;
    }

    /**
     * Projects the type, its record components and its members, recursively, from the children.
     *
     * @return the immutable declaration
     */
    @Override
    public TypeDeclaration declaration() {
        List<RecordComponentDeclaration> components = new ArrayList<>();
        List<Declaration>                members    = new ArrayList<>();
        for (Node child : getChildren()) {
            if (child instanceof DeclarationNode declarationNode) {
                Declaration declaration = declarationNode.declaration();
                if (declaration instanceof RecordComponentDeclaration component) {
                    components.add(component);
                } else {
                    members.add(declaration);
                }
            }
        }
        return new TypeDeclaration(header.kind(), header.name(), header.qualifiedName(), header.symbolId(), header.modifiers(),
                header.annotations(), header.typeParameters(), header.extendedTypes(), header.implementedTypes(),
                header.permittedTypes(), components, members, header.signature(), header.signatureSpan(), header.nameSpan(), header.bodySpan(),
                header.span(), header.javaDoc());
    }

    @Override
    protected void dispatch(JavaNodeVisitor visitor) {
        visitor.visit(this);
    }
}
