package org.jmouse.template.node;

import org.jmouse.template.lexer.Token;

import static org.jmouse.util.helper.Strings.underscored;

/**
 * Represents a basic implementation of a {@link Node} with an associated {@link Token}.
 *
 * <p>This class is designed to create hierarchical structures based on tokenized input.
 * It extends {@link AbstractNode}, providing a simple way to wrap tokens into nodes.</p>
 *
 * <pre>{@code
 * Token token = new Token("example", Type.T_IDENTIFIER, 1, 5, 2);
 * BasicNode node = BasicNode.forToken(token);
 * System.out.println(node.token()); // Outputs the associated token
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BasicNode extends AbstractNode {

    protected final Token token; // Associated token for this node

    /**
     * Default constructor for internal use only.
     */
    private BasicNode() {
        this(null);
    }

    /**
     * Constructs a {@code BasicNode} with the given token.
     *
     * @param token the token associated with this node
     */
    private BasicNode(Token token) {
        this.token = token;
    }

    /**
     * Creates a new {@code BasicNode} instance for a given token.
     *
     * @param token the token to wrap in a node
     * @return a new {@code BasicNode} instance
     */
    public static BasicNode forToken(Token token) {
        return new BasicNode(token);
    }

    /**
     * Returns the token associated with this node.
     *
     * @return the associated token
     */
    public Token token() {
        return this.token;
    }

    /**
     * Returns a string representation of this node.
     *
     * @return a formatted string representation of the node
     */
    @Override
    public String toString() {
        return "%s ENTRY: [%s]".formatted(underscored(getClass().getSimpleName(), true), token);
    }
}
