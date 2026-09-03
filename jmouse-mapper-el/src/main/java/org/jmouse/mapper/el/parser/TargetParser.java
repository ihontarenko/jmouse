package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.RefuseNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.node.UnmappedNode;

/**
 * Reads {@code target Type { … }} — everything about building one type.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.TARGET)
public class TargetParser extends JmmBlockParser<TargetNode, JmmToken> {

    @Override
    protected TargetNode createNode(TokenCursor cursor, ParserContext context) {
        TargetNode target = new TargetNode();

        target.setTargetType(TypeNames.read(cursor));

        return target;
    }

    @Override
    protected JmmToken token() {
        return JmmToken.T_TARGET;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_TARGET) && !cursor.isNext(BasicToken.T_COLON);
    }

    @Override
    protected void parseBody(TokenCursor cursor, TargetNode node, ParserContext context) {
        Node children = parseStatements(cursor, context);

        for (Node statement : children.getChildren()) {
            file(cursor, node, statement);
        }
    }

    /**
     * Files one statement onto the target.
     *
     * @param cursor    the cursor, for a failure's line
     * @param node      the target being filled
     * @param statement what dispatch produced
     */
    private void file(TokenCursor cursor, TargetNode node, Node statement) {
        switch (statement) {
            case UnmappedNode unmapped -> node.setUnmapped(unmapped.getValue());

            // ⚠️ A target refusal sits here because a target invariant holds whatever the source is. A
            // source refusal names one source type, so it belongs inside that source's block.
            case RefuseNode refusal -> {
                if (refusal.getSubject() != RefuseNode.Subject.TARGET) {
                    throw new JmmSyntaxException(cursor, "a 'refuse source' block names the source it "
                            + "is about, so it belongs inside the 'from' block for that source");
                }

                node.add(refusal);
            }

            // Inside a target the only rule block that can appear is the `always` one.
            case RuleBlockNode always -> {
                if (node.getAlways() != null) {
                    throw new JmmSyntaxException(cursor, "'%s' already has an 'always' block"
                            .formatted(node.getTargetType()));
                }

                node.setAlways(always);
            }
            case FromNode source -> node.add(source);

            default -> throw new JmmSyntaxException(cursor,
                    "a target holds 'unmapped', 'refuse', 'always' and 'from' — rules go inside one of "
                    + "the last two");
        }

        if (statement instanceof org.jmouse.el.node.Expression expression) {
            node.addExpression(expression);
        }
    }
}
