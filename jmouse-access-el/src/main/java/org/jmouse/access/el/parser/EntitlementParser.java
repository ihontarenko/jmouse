package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.EntitlementNode;
import org.jmouse.access.policy.model.PolicyEntitlement;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.core.Priority;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.lexer.BasicToken.T_AT;
import static org.jmouse.el.lexer.BasicToken.T_COLON;
import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_INT;
import static org.jmouse.el.lexer.BasicToken.T_NUMERIC;
import static org.jmouse.el.lexer.BasicToken.T_LONG;
import static org.jmouse.el.lexer.BasicToken.T_MINUS;
import static org.jmouse.el.lexer.BasicToken.T_OPEN_CURLY;
import static org.jmouse.el.lexer.BasicToken.T_STRING;

/**
 * Parses one line of an {@code entitlements} block:
 * {@code @ORGANIZATION:acme allow custody from 2026-08-01 until 2026-09-01 reason "…"}.
 *
 * <p>Four shapes, one per {@link PolicyEntitlement.Kind}, all addressed at a place written the same
 * way a grant's scope is. The trailing clauses may be written in any order for the same reason a
 * bundle header's are: a fixed sequence of optional words is a rule nobody remembers.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.ENTITLEMENT)
public class EntitlementParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        EntitlementNode node = new EntitlementNode();

        node.setSpan(SourceReader.span(cursor));
        node.setPlace(readPlace(cursor));
        node.setKind(readKind(cursor));
        node.setSubject(SourceReader.hyphenatedName(cursor));

        if (cursor.consumeIf(AccessToken.T_UNLIMITED)) {
            node.setUnlimited(true);
        } else if (cursor.isCurrent(T_NUMERIC, T_INT, T_LONG)) {
            node.setQuantity(cursor.ensure(T_NUMERIC, T_INT, T_LONG).value());
        }

        readQualifiers(cursor, node);
        parent.add(node);
    }

    private PolicyScope readPlace(TokenCursor cursor) {
        cursor.ensure(T_AT);

        String kind = cursor.ensure(T_IDENTIFIER).value();

        if (!cursor.consumeIf(T_COLON)) {
            return PolicyScope.kind(kind);
        }

        return PolicyScope.of(kind, SourceReader.hyphenatedName(cursor));
    }

    private PolicyEntitlement.Kind readKind(TokenCursor cursor) {
        if (cursor.consumeIf(AccessToken.T_PLAN)) {
            return PolicyEntitlement.Kind.PLAN;
        }
        if (cursor.consumeIf(AccessToken.T_TRIAL)) {
            return PolicyEntitlement.Kind.TRIAL;
        }
        if (cursor.consumeIf(AccessToken.T_ALLOW)) {
            return PolicyEntitlement.Kind.ALLOW;
        }
        if (cursor.consumeIf(AccessToken.T_DENY)) {
            return PolicyEntitlement.Kind.DENY;
        }

        Token token = cursor.peek();

        throw new PolicyParseException(
                SourceReader.span(cursor, token).toSourceSpan(),
                ("an entitlement says 'plan', 'trial', 'allow' or 'deny', and '%s' is none of them. "
                 + "⚠️ The effect is always written here — a line that granted by default would make "
                 + "a missing word into a silent allow").formatted(token.value())
        );
    }

    /**
     * Reads {@code from}, {@code until} and {@code reason} in whatever order they were written.
     *
     * <p>⚠️ Stops at the end of the line rather than at a keyword it does not know, and refuses
     * anything else by name — a skipped word would be left for the next parser and would fail a line
     * away from the typo.
     */
    private void readQualifiers(TokenCursor cursor, EntitlementNode node) {
        while (!atEndOfLine(cursor)) {
            if (cursor.consumeIf(AccessToken.T_FROM)) {
                node.setFrom(readDate(cursor));
            } else if (cursor.consumeIf(AccessToken.T_UNTIL)) {
                node.setUntil(readDate(cursor));
            } else if (cursor.consumeIf(AccessToken.T_REASON)) {
                node.setReason(SourceReader.literal(cursor.ensure(T_STRING)));
            } else {
                throw unexpected(cursor);
            }
        }
    }

    /**
     * Reads {@code 2026-09-12}, which the lexer delivers as a number and two hyphenated numbers.
     *
     * <p>⚠️ Kept as the text the file wrote. Turning it into a date here would put a calendar in a
     * parser and would decide a timezone nobody asked it to; binding does that, where a malformed one
     * can fail by name.
     */
    private String readDate(TokenCursor cursor) {
        if (cursor.isCurrent(T_STRING)) {
            return SourceReader.literal(cursor.ensure(T_STRING));
        }

        StringBuilder date = new StringBuilder(cursor.ensure(T_NUMERIC, T_INT, T_LONG).value());

        while (cursor.isCurrent(T_MINUS)) {
            cursor.ensure(T_MINUS);
            date.append('-').append(cursor.ensure(T_NUMERIC, T_INT, T_LONG, T_IDENTIFIER).value());
        }

        return date.toString();
    }

    private boolean atEndOfLine(TokenCursor cursor) {
        return !cursor.hasNext()
                || cursor.isCurrent(org.jmouse.el.lexer.BasicToken.T_NEW_LINE,
                                    org.jmouse.el.lexer.BasicToken.T_EOL,
                                    org.jmouse.el.lexer.BasicToken.T_SEMICOLON,
                                    org.jmouse.el.lexer.BasicToken.T_CLOSE_CURLY,
                                    T_AT, T_OPEN_CURLY);
    }

    private PolicyParseException unexpected(TokenCursor cursor) {
        Token token = cursor.peek();

        return new PolicyParseException(
                SourceReader.span(cursor, token).toSourceSpan(),
                ("an entitlement takes 'from', 'until' and 'reason', and '%s' is none of them")
                        .formatted(token.value())
        );
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.entitlement().matches(cursor);
    }

}
