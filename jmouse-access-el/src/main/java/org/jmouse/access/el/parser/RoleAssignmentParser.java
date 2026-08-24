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
import static org.jmouse.access.el.lexer.AccessToken.T_WHEN;
import static org.jmouse.access.el.lexer.AccessToken.T_REASON;
import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses {@code grants SPACE_ADMIN @SPACE:kyiv [when …]} — a role assignment inside a subject.
 *
 * <h2>⚠️ Why a condition is allowed <em>here</em></h2>
 *
 * <p>It reads as the sentence it is — <em>you hold this role when X</em> — and it lands exactly where
 * the design already puts conditions: a role says what a permission is <strong>worth</strong>, and
 * who holds it, where, and under what circumstances is a decision about <strong>one account</strong>.
 * An assignment is that decision, so nothing about the role itself changes and the same role assigned
 * to somebody else is untouched.
 *
 * <p>The condition distributes over every entry of the bundle, and composes with any condition an
 * entry carries of its own — both were written down, so both apply.
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

        if (cursor.consumeIf(T_WHEN)) {
            node.setCondition(ConditionReader.read(cursor));
        }

        // ⚠️ With or without a `when`, the same way GrantParser takes it: an assignment that is simply
        // absent is as opaque to whoever expected it as one that a condition switched off.
        if (cursor.consumeIf(T_REASON)) {
            node.setReason(SourceReader.literal(cursor.ensure(T_STRING)));
        }

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.roleAssignment().matches(cursor);
    }

}
