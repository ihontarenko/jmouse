package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.RoleAssignmentNode;
import org.jmouse.access.el.node.SingleScopeNode;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.access.el.lexer.AccessToken.T_GRANTS;
import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses {@code grants SPACE_ADMIN @SPACE:kyiv} — a role assignment inside a subject.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.ROLE_ASSIGNMENT)
public class RoleAssignmentParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        RoleAssignmentNode node   = new RoleAssignmentNode();
        SingleScopeParser  parser = (SingleScopeParser) context.getParser(SingleScopeParser.class);

        node.setSpan(SourceReader.span(cursor));

        cursor.ensure(T_GRANTS);

        node.setRoleName(SourceReader.literal(cursor.ensure(T_IDENTIFIER, T_STRING)));
        node.setScope((SingleScopeNode) parser.parse(cursor, context));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.roleAssignment().matches(cursor);
    }

}
