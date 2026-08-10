package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.GrantNode;
import org.jmouse.access.el.node.PermissionValueNode;
import org.jmouse.access.el.node.SingleScopeNode;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ExpressionParser;
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
 * <p>⚠️ <strong>The condition is captured as raw text.</strong> It is parsed only far enough to know
 * where it ends, then sliced straight out of the source. Rendering it back from the tree would give
 * a different spelling of the same expression — different spacing, different parentheses — and the
 * administrator reading it in the control room would not find that line in the file.</p>
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
            node.setCondition(readCondition(cursor, context));
        }

        parent.add(node);
    }

    /**
     * Reads the condition after {@code when}, and returns it exactly as it was typed.
     *
     * <p>⚠️ It is read with the <em>full</em> expression parser rather than the operator parser
     * alone. The two disagree about the ternary, and the one that stops short does not fail — it
     * slices the condition at the {@code ?}, leaves the rest of the line to be misread, and the
     * grant then answers a question nobody wrote. Which expressions a condition may actually
     * <em>use</em> is settled by the restricted dialect at compile time, not by reading less of the
     * line than was written.</p>
     *
     * @param cursor  the cursor, positioned on the first token of the condition
     * @param context the parser context, for the expression parser that finds the condition's end
     * @return the condition source, verbatim
     */
    private String readCondition(TokenCursor cursor, ParserContext context) {
        ExpressionParser parser = (ExpressionParser) context.getParser(ExpressionParser.class);
        Token            first  = cursor.current();

        parser.parse(cursor, context);

        return SourceReader.text(cursor, first, cursor.lookAt(-1));
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.grant().matches(cursor);
    }

}
