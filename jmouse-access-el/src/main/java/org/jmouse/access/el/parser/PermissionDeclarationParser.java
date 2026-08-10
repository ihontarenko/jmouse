package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.PermissionDeclarationNode;
import org.jmouse.access.el.node.PermissionValueNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses one line of a {@code permissions} block: {@code form:read "Read forms"}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.PERMISSION_DECLARATION)
public class PermissionDeclarationParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        PermissionDeclarationNode node   = new PermissionDeclarationNode();
        PermissionValueParser     parser = (PermissionValueParser) context.getParser(PermissionValueParser.class);

        node.setSpan(SourceReader.span(cursor));
        node.setName(((PermissionValueNode) parser.parse(cursor, context)).getPermission());
        node.setDescription(SourceReader.literal(cursor.ensure(T_STRING)));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.permissionDeclaration().matches(cursor);
    }

}
