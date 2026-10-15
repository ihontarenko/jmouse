package org.jmouse.java.parser.node;

import org.jmouse.java.parser.model.ImportDeclaration;

/**
 * One import declaration. A leaf.
 */
public final class ImportNode extends JavaNode {

    private final ImportDeclaration declaration;

    /**
     * Creates the node.
     *
     * @param declaration the declaration
     */
    public ImportNode(ImportDeclaration declaration) {
        super(declaration.span());
        this.declaration = declaration;
    }

    /**
     * Returns the declaration.
     *
     * @return the import declaration
     */
    public ImportDeclaration declaration() {
        return declaration;
    }

    @Override
    protected void dispatch(JavaNodeVisitor visitor) {
        visitor.visit(this);
    }
}
