package org.jmouse.spice.expression;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.node.expression.literal.DoubleLiteralNode;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.FunctionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PrimaryExpressionParser;
import org.jmouse.spice.SpiceNumbers;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * SPICE's primary expressions: a number with its scale factor, a name, a call, a group.
 *
 * <p>A number is one word however jME's lexer cut it: {@code 10k} reaches here as {@code 10} and {@code k},
 * {@code .5} as {@code .} and {@code 5}, {@code 2.2µ} as {@code 2.2} and a sign it does not know. The tokens that touch
 * are joined back into the word as written and read by {@link SpiceNumbers}, so {@code 1meg} is a million and
 * {@code 10kOhm} ten thousand, exactly as a SPICE value is. Every name and call carries where it stands, so a refusal
 * further on can say so.</p>
 */
final class SpicePrimaryExpressionParser extends PrimaryExpressionParser {

    /** What ends a number's word where it touches it: an operator, a bracket, a separator. */
    private static final Set<String> SEPARATORS = Set.of("+", "-", "*", "/", "^", "**", "(", ")", ",", "<", ">", "<=",
            ">=", "==", "!=", "=", "&&", "||", "!", "?", ":");

    private static final Set<BasicToken> NUMBERS = Set.of(BasicToken.T_NUMERIC, BasicToken.T_INT, BasicToken.T_LONG,
            BasicToken.T_FLOAT, BasicToken.T_DOUBLE, BasicToken.T_SHORT, BasicToken.T_BYTE);

    /** A name as SPICE writes one; jME lexes some of them as its own words ({@code in}, {@code eq}), which here they are not. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (!cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL)) {
            throw new SpiceExpressionRefusal(cursor.hasNext() ? cursor.peek().offset() : -1,
                    "the expression ends where a value was expected");
        }
        Token token = cursor.peek();
        if (startsNumber(cursor)) {
            parent.add(number(cursor));
        } else if (NAME.matcher(token.value()).matches() && cursor.isNext(BasicToken.T_OPEN_PAREN)) {
            cursor.next();
            parent.add(spanned(FunctionParser.call(token.value(), cursor, context), token));
        } else if (NAME.matcher(token.value()).matches()) {
            cursor.next();
            parent.add(spanned(new PropertyNode(token.value()), token));
        } else if (cursor.consumeIf(BasicToken.T_OPEN_PAREN)) {
            parent.add(context.getParser(ExpressionParser.class).parse(cursor, context));
            if (!cursor.consumeIf(BasicToken.T_CLOSE_PAREN)) {
                throw new SpiceExpressionRefusal(cursor.hasNext() ? cursor.peek().offset() : token.offset(),
                        "a parenthesis opened here is never closed");
            }
        } else {
            throw new SpiceExpressionRefusal(token.offset(), "'" + token.value() + "' is not part of an expression");
        }
    }

    /** A number's first token: its digits, or the point a fraction like {@code .5} opens with. */
    private static boolean startsNumber(TokenCursor cursor) {
        Token token = cursor.peek();
        if (token.type() instanceof BasicToken basic && NUMBERS.contains(basic)) {
            return true;
        }
        return token.type() == BasicToken.T_DOT && SourceReading.touches(cursor, token, cursor.lookAt(1))
                && cursor.lookAt(1).type() instanceof BasicToken next && NUMBERS.contains(next);
    }

    private static Node number(TokenCursor cursor) {
        Token first = cursor.peek();
        String word = SourceReading.touchingWord(cursor, SEPARATORS);
        SpiceNumbers.Engineering number = SpiceNumbers.read(word).orElseThrow(
                () -> new SpiceExpressionRefusal(first.offset(), "'" + word + "' is not a number"));
        return spanned(new DoubleLiteralNode(number.value()), first);
    }

    private static <T extends AbstractExpression> T spanned(T node, Token token) {
        node.setSpan(SpanNode.of(token.lineNumber(), token.offset()));
        return node;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return false;
    }
}
