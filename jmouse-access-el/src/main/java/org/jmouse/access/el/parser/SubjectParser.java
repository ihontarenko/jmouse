package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.SubjectNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PlaceholderParser;

import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses {@code subject u-42 { … }}.
 *
 * <p>The id may be a bare identifier, a quoted string or a {@code ${placeholder}}. ⚠️ Quote anything
 * a bare identifier cannot hold — the lexer reads a bare name as letters, digits and underscores, so
 * an account id carrying a hyphen has to be written {@code subject 'u-42'}. A placeholder is stored
 * verbatim: resolving one needs a property source, which is exactly the dependency this stage keeps
 * out.</p>
 *
 * <p>It may also be {@code *}, which means <strong>every account</strong> — {@code subject * { … }}.
 * ⚠️ The parser reports it and decides nothing about it: {@code PolicyBinder} is what refuses
 * everything in such a block except a denial, because a rule that quietly applied to everybody is the
 * one nobody would notice writing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.SUBJECT)
public class SubjectParser extends PolicyBlockParser<SubjectNode, AccessToken> {

    @Override
    protected SubjectNode createNode(TokenCursor cursor, ParserContext context) {
        return new SubjectNode(readIdentity(cursor, context));
    }

    /**
     * Reads the subject's id in whichever of its three forms was written.
     *
     * @param cursor  the cursor, positioned on the id
     * @param context the parser context, for the placeholder parser
     * @return the id, unquoted, or the placeholder exactly as it was written
     */
    private String readIdentity(TokenCursor cursor, ParserContext context) {
        if (cursor.isCurrent(T_DOLLAR)) {
            Expression placeholder = (Expression) context.getParser(PlaceholderParser.class).parse(cursor, context);
            return placeholder.toSource();
        }

        return SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING, T_MULTIPLY));
    }

    @Override
    protected AccessToken token() {
        return AccessToken.T_SUBJECT;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.subject().matches(cursor);
    }

}
