package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.GrantNode;
import org.jmouse.access.el.node.PermissionValueNode;
import org.jmouse.access.el.node.SingleScopeNode;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
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

    /**
     * Where a condition stops: the end of the line, or the brace that closes the block it is in.
     *
     * <p>The closing brace is here because a grant may be the last line before it with nothing
     * between them, and because the condition dialect has no braces of its own — so one appearing
     * here can only be the block's.
     */
    private static final Token.Type[] TERMINATORS = {
            BasicToken.T_NEW_LINE, BasicToken.T_EOL, BasicToken.T_SEMICOLON, BasicToken.T_CLOSE_CURLY,
    };

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
            node.setCondition(readCondition(cursor));
        }

        parent.add(node);
    }

    /**
     * Reads the condition after {@code when}, and returns it exactly as it was typed.
     *
     * <p>⚠️ <strong>It is not parsed here, and that is the fix rather than a shortcut.</strong> This
     * used to run the full expression parser over the cursor to find where the condition ends — and
     * the result was thrown away, because the text is sliced out of the source either way and
     * {@code ExpressionConditionCompiler} re-lexes and compiles it later with a lexer of its own. A
     * whole parse was being paid for a delimiter.
     *
     * <p>Worse, it was the <em>wrong</em> parse. These tokens came out of {@link org.jmouse.access.el.lexer.AccessRecognizer},
     * which turns this grammar's ten keywords into keywords <em>wherever they appear</em> — so a
     * perfectly ordinary condition naming the caller, a role or a plan arrived at the expression
     * parser as {@code T_SUBJECT}, {@code T_ROLE}, {@code T_PLAN}, and {@code PrimaryExpressionParser}
     * takes only {@code T_IDENTIFIER}. The file then failed to <em>parse</em> over a word that is not
     * part of its grammar at that position at all, with a message about a token nobody wrote.
     *
     * <p>So the end of a condition is found the way every other statement in this grammar finds it:
     * by running to the end of the line. A grant is one line, the restricted dialect has no braces
     * and no statement separators, and nothing it may contain can span a newline — so there is no
     * expression this reads too little of, and no keyword left to trip over.
     *
     * @param cursor the cursor, positioned on the first token of the condition
     * @return the condition source, verbatim
     */
    private String readCondition(TokenCursor cursor) {
        Token first = cursor.current();
        Token last  = first;

        while (cursor.hasNext() && !cursor.isCurrent(TERMINATORS)) {
            last = cursor.next();
        }

        return SourceReader.text(cursor, first, last);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.grant().matches(cursor);
    }

}
