package org.jmouse.java.parser.node;

import org.jmouse.java.parser.model.PackageDeclaration;

/**
 * The package declaration of a file. A leaf.
 */
public final class PackageNode extends JavaNode {

    private final PackageDeclaration declaration;

    /**
     * Creates the node.
     *
     * @param declaration the declaration
     */
    public PackageNode(PackageDeclaration declaration) {
        super(declaration.span());
        this.declaration = declaration;
    }

    /**
     * Returns the declaration.
     *
     * @return the package declaration
     */
    public PackageDeclaration declaration() {
        return declaration;
    }

    @Override
    protected void dispatch(JavaNodeVisitor visitor) {
        visitor.visit(this);
    }
}
