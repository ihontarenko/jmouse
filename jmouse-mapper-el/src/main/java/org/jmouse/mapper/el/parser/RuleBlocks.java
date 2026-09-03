package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.mapper.el.node.IncludeNode;
import org.jmouse.mapper.el.node.LetNode;
import org.jmouse.mapper.el.node.RefuseNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;

/**
 * Sorting a parsed block's statements into the three things a rule block holds. 🗃️
 *
 * <p>Three parsers read a body of rules — {@code always}, a {@code fragment}, and the rule half of a
 * {@code from} — and dispatch hands each of them the same three node types. Written once, because the
 * fourth block anybody adds will want the same, and because the duplicate-rule refusal below is the
 * kind of message that drifts when it is written three times.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RuleBlocks {

    private RuleBlocks() {
    }

    /**
     * Files one statement into a rule block.
     *
     * @param cursor    the cursor, for a failure's line
     * @param block     the block being filled
     * @param statement what dispatch produced
     * @throws JmmSyntaxException when a rule block cannot hold it
     */
    public static void file(TokenCursor cursor, RuleBlockNode block, Node statement) {
        switch (statement) {
            case RuleNode rule -> {
                if (block.add(rule) != null) {
                    throw new JmmSyntaxException(cursor,
                            "'%s' already has a rule in this block".formatted(rule.getProperty()));
                }
            }
            case LetNode binding -> block.add(binding);
            case IncludeNode include -> block.include(include);

            // ⚠️ Said on its own terms rather than left to fall through. A refusal read as a rule is
            // reported as a missing colon, naming tokens nobody typed — which is what the hand-written
            // reader did before it grew this same message.
            case RefuseNode ignored -> throw new JmmSyntaxException(cursor,
                    "a 'refuse' block is not a rule and cannot stand between rules — it opens the block "
                    + "it guards, so that whoever reads the file meets what stops the mapping before "
                    + "what performs it. Move it above the first rule of the 'from' whose source it "
                    + "names, or out to target level when it is about the target");

            default -> throw new JmmSyntaxException(cursor,
                    "'%s' is not something a block of rules holds"
                            .formatted(statement.getClass().getSimpleName()));
        }

        if (statement instanceof Expression expression) {
            block.addExpression(expression);
        }
    }

    /**
     * Files every statement of a parsed body.
     *
     * @param cursor   the cursor, for a failure's line
     * @param block    the block being filled
     * @param children what {@code StatementsParser} produced
     */
    public static void fill(TokenCursor cursor, RuleBlockNode block, Node children) {
        for (Node statement : children.getChildren()) {
            file(cursor, block, statement);
        }
    }
}
