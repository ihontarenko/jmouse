package org.jmouse.el.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.PropertyNode;

import static org.jmouse.el.lexer.BasicToken.*;

/**
 * A property path — {@code order.customer.name}, {@code entry[quantity]}, {@code meta[stock.after]}.
 *
 * <h2>⚠️ A dot after a dot is a step; a dot INSIDE brackets is part of a name</h2>
 *
 * <p>The two halves of a path are read by different rules, and conflating them is the bug this class
 * carried. Between dots, each segment is one identifier and the dot is the step. Inside brackets there
 * is no stepping at all: whatever is written there is a <strong>single stored name</strong> handed to
 * whoever resolves the path, and a stored name may perfectly well contain a dot.</p>
 *
 * <p>⚠️ <strong>It used to consume exactly one token between the brackets</strong> and then demand the
 * closing one, so {@code entry[quantity]} read correctly and anything namespaced did not. Innoventa
 * names every audit detail after its module — {@code stock.after}, {@code project.quantity} — and every
 * one of them was refused with <em>expected {@code T_CLOSE_BRACKET}, but encountered {@code T_DOT}</em>.
 * The feature that depended on it was documented, shipped, and had never once worked; nothing failed
 * loudly enough to say so, because a filter nobody could write is a filter nobody reported.</p>
 *
 * <p>⚠️ <strong>Quoting was not a way round it.</strong> {@code meta["stock.after"]} parses — one token —
 * and produces the path {@code meta["stock.after"]}, quotes included, which no schema registers. So a
 * dotted key was unreachable in every spelling rather than merely awkward in one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PropertyParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token         token   = cursor.next();
        StringBuilder builder = new StringBuilder();

        builder.append(token.value());

        while (cursor.isCurrent(T_DOT, T_OPEN_BRACKET)) {
            if (cursor.isCurrent(T_OPEN_BRACKET)) {
                bracketed(cursor, builder);
            } else {
                // A step: the dot, then the one identifier after it.
                builder.append(cursor.next().value());
                builder.append(cursor.next().value());
            }
        }

        parent.add(new PropertyNode(builder.toString()));
    }

    /**
     * Everything between {@code [} and {@code ]}, verbatim.
     *
     * <p>⚠️ <strong>Every token, not one.</strong> The name inside is the store's, not the language's —
     * see the class note. Joining the tokens back together is what makes {@code meta[stock.after]} the
     * path {@code meta[stock.after]} rather than a syntax error at the dot.</p>
     *
     * <p>⚠️ <strong>An unclosed bracket still fails, and it has to.</strong> {@code cursor.ensure} is
     * what raises it; a loop that simply stopped at the end of the input would turn a typo into a path
     * nobody wrote, resolved against a schema that would then report it as an unknown attribute — the
     * error one step further from its cause.</p>
     */
    private static void bracketed(TokenCursor cursor, StringBuilder builder) {
        builder.append(cursor.next().value());

        while (!cursor.isCurrent(T_CLOSE_BRACKET) && cursor.hasNext()) {
            builder.append(cursor.next().value());
        }

        builder.append(cursor.ensure(T_CLOSE_BRACKET).value());
    }

}
