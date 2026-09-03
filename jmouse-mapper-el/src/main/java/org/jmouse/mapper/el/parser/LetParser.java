package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.LetNode;

/**
 * Reads {@code let name = expression} — a sub-expression named for reuse within one block.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.LET)
public class LetParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        LetNode binding = new LetNode();

        binding.setSpan(JmmSpans.at(cursor));

        cursor.ensure(JmmToken.T_LET);
        binding.setName(cursor.ensure(JmmToken.nameTokens()).value());
        cursor.ensure(BasicToken.T_EQ);
        binding.setExpression(RuleValueReader.read(cursor));

        parent.add(binding);
    }

    /**
     * Whether this line binds a name — {@code let x = …} rather than a property called {@code let}.
     *
     * <p>⚠️ The third token settles it: a property called {@code let} is followed by a colon, and a
     * binding never is.</p>
     */
    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_LET)
               && cursor.isNext(JmmToken.nameTokens())
               && !cursor.checkAt(2, BasicToken.T_COLON);
    }
}
