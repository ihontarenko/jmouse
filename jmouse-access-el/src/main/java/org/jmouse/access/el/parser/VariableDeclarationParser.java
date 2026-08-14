package org.jmouse.access.el.parser;

import org.jmouse.access.VariableKind;
import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.VariableDeclarationNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses one line of a {@code variables} block:
 * {@code dynamic ambientType "What the workspace this call is in counts"}.
 *
 * <p>The description is optional, for {@link ActionDeclarationParser}'s reason: a name a rule may
 * mention is worth declaring even where nobody has written the sentence yet, and a grammar that
 * refuses the line teaches people to write a placeholder sentence instead.</p>
 *
 * <p>⚠️ Unlike a capability's kind, this one is mapped here rather than carried as the word that was
 * written. There are two spellings and both are the language's own keywords, so nothing a product
 * chose can be lost in the mapping — and the node then holds the same {@link VariableKind} the
 * publishing side does, which is what lets the two be compared at all.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.VARIABLE_DECLARATION)
public class VariableDeclarationParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        VariableDeclarationNode node = new VariableDeclarationNode();
        Token                   kind = cursor.ensure(AccessToken.T_CONSTANT, AccessToken.T_DYNAMIC);

        node.setSpan(SourceReader.span(cursor));
        node.setKind(kindOf(kind));
        node.setName(SourceReader.hyphenatedName(cursor));

        if (cursor.isCurrent(T_STRING)) {
            node.setDescription(SourceReader.literal(cursor.ensure(T_STRING)));
        }

        parent.add(node);
    }

    private static VariableKind kindOf(Token keyword) {
        return keyword.type() == AccessToken.T_DYNAMIC ? VariableKind.DYNAMIC : VariableKind.CONSTANT;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.variableDeclaration().matches(cursor);
    }

}
