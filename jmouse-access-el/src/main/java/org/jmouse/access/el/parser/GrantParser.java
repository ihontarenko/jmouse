package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.GrantNode;
import org.jmouse.access.el.node.PermissionValueNode;
import org.jmouse.access.el.node.SingleScopeNode;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import org.jmouse.el.node.Node;

import static org.jmouse.access.el.lexer.AccessToken.*;

/**
 * Parses one scoped permission: {@code @SCOPE[:instance] permission [allow|deny] [when …]}.
 *
 * <p>This is the only parser for that shape. Inside a {@code role} the same tokens mean a bundle
 * entry and inside a {@code subject} they mean a grant, but they are the same tokens either way and
 * no lookahead can tell them apart — so they parse once and
 * {@link org.jmouse.access.el.node.RoleNode} narrows the reading where it applies.</p>
 *
 * <p>⚠️ <strong>The condition is captured as raw text</strong> by {@link ConditionReader}, which a
 * role assignment shares. It is parsed only far enough to know where it ends, then sliced straight
 * out of the source: rendering it back from a tree would give a different spelling of the same
 * expression — different spacing, different parentheses — and the administrator reading it in the
 * control room would not find that line in the file.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.GRANT)
public class GrantParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        GrantNode             node        = new GrantNode();
        SingleScopeParser     scopeParser = (SingleScopeParser) context.getParser(SingleScopeParser.class);
        PermissionValueParser valueParser = (PermissionValueParser) context.getParser(PermissionValueParser.class);

        node.setSpan(SourceReader.span(cursor));
        node.setScope((SingleScopeNode) scopeParser.parse(cursor, context));
        node.setPermission(((PermissionValueNode) valueParser.parse(cursor, context)).getPermission());

        if (cursor.isCurrent(T_ALLOW, T_DENY)) {
            node.setEffect(PolicyEffect.valueOf(cursor.ensure(T_ALLOW, T_DENY).value().toUpperCase()));
        }

        if (cursor.consumeIf(T_WHEN)) {
            node.setCondition(ConditionReader.read(cursor));
        }

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.grant().matches(cursor);
    }

}
