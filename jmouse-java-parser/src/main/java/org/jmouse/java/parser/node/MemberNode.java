package org.jmouse.java.parser.node;

import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.TypeDeclaration;

/**
 * A member that holds no declarations of its own: a field variable, method, constructor, enum constant,
 * initializer or record component. A leaf — its body, when it has one, is an opaque span.
 */
public final class MemberNode extends JavaNode implements DeclarationNode {

    private final Declaration declaration;

    /**
     * Creates the node.
     *
     * @param declaration a declaration that is not a type
     */
    public MemberNode(Declaration declaration) {
        super(declaration.span());
        if (declaration instanceof TypeDeclaration) {
            throw new IllegalArgumentException("A type is a TypeNode, not a member node.");
        }
        this.declaration = declaration;
    }

    @Override
    public Declaration declaration() {
        return declaration;
    }

    @Override
    protected void dispatch(JavaNodeVisitor visitor) {
        visitor.visit(this);
    }
}
