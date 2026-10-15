package org.jmouse.java.parser.node;

import org.jmouse.java.parser.model.ModuleDeclaration;

/**
 * The module declaration of a {@code module-info.java}. A leaf: its directives are an opaque body.
 */
public final class ModuleNode extends JavaNode {

    private final ModuleDeclaration declaration;

    /**
     * Creates the node.
     *
     * @param declaration the declaration
     */
    public ModuleNode(ModuleDeclaration declaration) {
        super(declaration.span());
        this.declaration = declaration;
    }

    /**
     * Returns the declaration.
     *
     * @return the module declaration
     */
    public ModuleDeclaration declaration() {
        return declaration;
    }

    @Override
    protected void dispatch(JavaNodeVisitor visitor) {
        visitor.visit(this);
    }
}
