package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.type.TypeAnnotations;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.StatementMatcher;
import org.jmouse.script.el.lexer.StatementToken;
import org.jmouse.script.el.node.DeclarationNode;

/**
 * Parses {@code let slot : int = @store.next_slot(entry)}, {@code let c : current} and {@code const limit = 10};
 * {@code local} is {@code let}'s other spelling, read the same way. {@code state hits : int = 0} and
 * {@code parameter limit : int = 10} are the document's, bound once per context — see {@link DeclarationNode.Kind}.
 *
 * <p>The keyword in front is what makes the {@code =} unambiguous here — the reason an assignment needs a check that
 * this does not. The type is resolved while parsing: a misspelt one is refused at its line.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(StatementPriority.DECLARATION)
public class DeclarationParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        DeclarationNode node = new DeclarationNode();

        node.setSpan(SourceReader.span(cursor));

        DeclarationNode.Kind contextual = contextualKind(cursor);
        if (contextual != null) {
            node.setKind(contextual);
            cursor.next();
        } else if (cursor.consumeIf(StatementToken.T_CONST)) {
            node.setKind(DeclarationNode.Kind.CONST);
        } else {
            cursor.ensure(StatementToken.T_LET);
        }
        node.setName(SourceReader.name(cursor));
        TypeAnnotations.read(cursor, context).ifPresent(node::setType);

        if (cursor.consumeIf(BasicToken.T_EQ)) {
            node.setValue(Expressions.read(cursor, context));
        } else if (node.getKind() == DeclarationNode.Kind.CONST) {
            throw new ParseException("The constant '" + node.getName() + "' needs a value: const "
                    + node.getName() + " = …");
        }

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return StatementMatcher.declaration().matches(cursor) || contextualKind(cursor) != null;
    }

    /**
     * {@code state} and {@code parameter} are declarations only where they open one: the word, a name, then {@code :}
     * or {@code =}. Anywhere else they are ordinary names — a host's event hands its scripts a {@code state}
     * (`on jobFinished when state == 'FAILED'`), and making the word a keyword would have broken every such file.
     */
    private static DeclarationNode.Kind contextualKind(TokenCursor cursor) {
        if (!cursor.isCurrent(BasicToken.T_IDENTIFIER) || !cursor.checkAt(1, StatementToken.nameTokens())
                || !cursor.checkAt(2, BasicToken.T_COLON, BasicToken.T_EQ)) {
            return null;
        }
        return switch (cursor.current().value()) {
            case "state" -> DeclarationNode.Kind.STATE;
            case "parameter" -> DeclarationNode.Kind.PARAMETER;
            default -> null;
        };
    }

}
