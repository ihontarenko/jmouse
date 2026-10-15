package org.jmouse.java.parser.node;

import org.jmouse.java.parser.model.Declaration;

/**
 * A node that declares a symbol: a type or a member.
 */
public sealed interface DeclarationNode permits TypeNode, MemberNode {

    /**
     * Returns the declaration as an immutable record. For a type this is a projection of the node's
     * current children.
     *
     * @return the declaration
     */
    Declaration declaration();
}
