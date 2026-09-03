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

/**
 * Reads {@code from Source { … }} or {@code from Source : expression}.
 *
 * <p>⚠️ A whole-pair conversion and a block of rules are alternatives, never both: a block beside a
 * conversion leaves no answer to what the rules were supposed to apply to. That is why
 * {@link #parseBlock} is overridden rather than {@code parseBody} — this is the one construction in the
 * language whose body may not be a block at all.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.FROM)
public class FromParser extends JmmBlockParser<FromNode, JmmToken> {

    @Override
    protected FromNode createNode(TokenCursor cursor, ParserContext context) {
        FromNode source = new FromNode();

        source.setSourceType(TypeNames.read(cursor));

        return source;
    }

    @Override
    protected JmmToken token() {
        return JmmToken.T_FROM;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_FROM) && !cursor.isNext(BasicToken.T_COLON);
    }

    /**
     * Reads whichever of the two shapes follows the source type.
     *
     * @param cursor  the cursor, on the colon of a conversion or the brace of a block
     * @param node    the source being filled
     * @param context the parser context
     */
    @Override
    protected void parseBlock(TokenCursor cursor, FromNode node, ParserContext context) {
        if (cursor.consumeIf(BasicToken.T_COLON)) {
            node.setConversion(RuleValueReader.read(cursor));

            return;
        }

        parseBody(cursor, node, context);
    }

    /**
     * Files the block's statements: at most one leading refusal, then rules.
     *
     * <p>⚠️ <strong>A refusal opens the block it guards and may not stand between rules.</strong> The
     * constraint is real and worth keeping — a reader meets what stops the mapping before what performs
     * it — and it is now checked against what was actually parsed rather than by refusing to parse
     * further. Which also means the message names the rule it came after.</p>
     */
    @Override
    protected void parseBody(TokenCursor cursor, FromNode node, ParserContext context) {
        Node          children = parseStatements(cursor, context);
        RuleBlockNode rules    = new RuleBlockNode();

        for (Node statement : children.getChildren()) {
            if (statement instanceof RefuseNode refusal) {
                file(cursor, node, rules, refusal);

                continue;
            }

            RuleBlocks.file(cursor, rules, statement);
        }

        node.setRules(rules);
    }

    /**
     * Files a refusal onto its source, refusing a second one and one that names the wrong subject.
     *
     * @param cursor  the cursor, for a failure's line
     * @param node    the source being filled
     * @param rules   what has been read so far, to tell a leading refusal from a stray one
     * @param refusal the block
     */
    private void file(TokenCursor cursor, FromNode node, RuleBlockNode rules, RefuseNode refusal) {
        if (refusal.getSubject() != RefuseNode.Subject.SOURCE) {
            throw new JmmSyntaxException(cursor, "a 'refuse target' block holds whatever the source is, "
                    + "so it belongs at target level rather than inside one 'from'");
        }

        if (node.getRefusal() != null) {
            throw new JmmSyntaxException(cursor, ("'from %s' already refuses its source — write every "
                    + "condition in the one block, since all of them are evaluated anyway")
                    .formatted(node.getSourceType()));
        }

        if (!rules.isEmpty()) {
            throw new JmmSyntaxException(cursor, "a 'refuse' block is not a rule and cannot stand "
                    + "between rules — it opens the block it guards, so that whoever reads the file "
                    + "meets what stops the mapping before what performs it");
        }

        node.setRefusal(refusal);
        node.addExpression(refusal);
    }
}
