package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.ActionDeclarationNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.el.lexer.BasicToken.T_COMMA;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses one line of an {@code actions} block:
 * {@code entry.listByPurpose "List submissions of one purpose" publishes purpose, tier}.
 *
 * <p>The {@code publishes} clause is optional: an action that carries nothing is still worth
 * declaring, because a rule may scope itself to it and compare nothing but the action's own name.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.ACTION_DECLARATION)
public class ActionDeclarationParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ActionDeclarationNode node = new ActionDeclarationNode();

        node.setSpan(SourceReader.span(cursor));
        node.setName(SourceReader.dottedName(cursor));
        node.setDescription(SourceReader.literal(cursor.ensure(T_STRING)));

        if (cursor.consumeIf(AccessToken.T_PUBLISHES)) {
            node.setValues(readValues(cursor));
        }

        parent.add(node);
    }

    /**
     * Reads {@code publishes purpose, tier} — the value names this action carries into a condition.
     *
     * <p>Names only, and deliberately: a name is what a rule mentions and therefore what a validator
     * can check a rule against. Anything more descriptive would be prose, and prose is what documents
     * that have stopped being true are written in.
     */
    private List<String> readValues(TokenCursor cursor) {
        List<String> values = new ArrayList<>();

        values.add(SourceReader.literal(cursor.ensure(AccessToken.nameTokens())));

        while (cursor.consumeIf(T_COMMA)) {
            values.add(SourceReader.literal(cursor.ensure(AccessToken.nameTokens())));
        }

        return values;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.actionDeclaration().matches(cursor);
    }

}
