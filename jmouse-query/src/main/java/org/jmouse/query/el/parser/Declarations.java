package org.jmouse.query.el.parser;

import org.jmouse.core.MimeParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.AttributeNode;
import org.jmouse.query.el.node.BagNode;
import org.jmouse.query.el.node.CollectionNode;
import org.jmouse.query.el.node.JoinNode;
import org.jmouse.query.el.node.ParameterDeclarationNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The pieces a {@code mapping} is made of, read once and shared.
 *
 * <h2>⚠️ Shared rather than copied, because a mapping has two spellings</h2>
 *
 * <p>The older {@code source { }} block and the current {@code structure} / {@code mapping} pair declare
 * the same things in the same words. A second implementation of {@code bag} or {@code join} would agree
 * with the first until somebody fixed a bug in one of them — and the reader tolerating an old spelling is
 * only safe while both spellings mean exactly one thing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class Declarations {

    private Declarations() {
    }

    /**
     * One segment of a name — and ⚠️ <strong>a keyword counts as a word here</strong>.
     *
     * <p>{@code issue.key}, {@code entry[value]}, {@code order.from} are perfectly ordinary things for a
     * product to call its data, and every one of them collides with a word this grammar spends elsewhere.
     * Refusing them would mean a language that dictates what a database may name its columns.</p>
     *
     * <p>It is unambiguous because a name only appears where a name can appear.</p>
     */
    static String word(TokenCursor cursor) {
        Token token = cursor.peek();

        if (token.type() == BasicToken.T_STRING) {
            cursor.next();

            return MimeParser.unquote(token.value());
        }

        if (token.type() == BasicToken.T_IDENTIFIER || token.type() instanceof QueryToken) {
            cursor.next();

            return token.value();
        }

        return cursor.ensure(BasicToken.T_IDENTIFIER, BasicToken.T_STRING).value();
    }

    /** An identifier or a quoted string — a stored name may be neither a word nor ASCII. */
    static String name(TokenCursor cursor) {
        return word(cursor);
    }

    /** What a query writes — {@code issue.key}, {@code entry[quantity]}. */
    static String path(TokenCursor cursor) {
        StringBuilder written = new StringBuilder(word(cursor));

        while (cursor.isCurrent(BasicToken.T_OPEN_BRACKET) || cursor.isCurrent(BasicToken.T_DOT)) {
            if (cursor.consumeIf(BasicToken.T_OPEN_BRACKET)) {
                written.append('[').append(word(cursor)).append(']');
                cursor.ensure(BasicToken.T_CLOSE_BRACKET);
            } else {
                cursor.ensure(BasicToken.T_DOT);
                written.append('.').append(word(cursor));
            }
        }

        return written.toString();
    }

    /**
     * A stored name that may name its table too — {@code statuses.category}.
     *
     * <p>⚠️ Only for what the STORE calls something, never for what a query writes.</p>
     */
    static String qualified(TokenCursor cursor) {
        StringBuilder written = new StringBuilder(word(cursor));

        while (cursor.isCurrent(BasicToken.T_DOT)) {
            cursor.ensure(BasicToken.T_DOT);
            written.append('.').append(word(cursor));
        }

        return written.toString();
    }

    /**
     * ⚠️ The colon is optional wherever a directive takes one.
     *
     * <p>The current spelling writes it; the older one did not. A reader refusing the older form would
     * turn every stored document into an error on the day the writer changed, which is exactly the
     * migration this language does not have.</p>
     */
    static void optionalColon(TokenCursor cursor) {
        cursor.consumeIf(BasicToken.T_COLON);
    }

    /** {@code bag: request_fields on request_id key field_code value text_value} */
    static BagNode bag(TokenCursor cursor) {
        BagNode bag = new BagNode();

        optionalColon(cursor);
        bag.setTable(name(cursor));

        cursor.ensure(QueryToken.T_ON);
        bag.setForeignKey(name(cursor));

        cursor.ensure(QueryToken.T_KEY);
        bag.setKeyColumn(name(cursor));

        cursor.ensure(QueryToken.T_VALUE);
        bag.setValueColumn(name(cursor));

        return bag;
    }

    /** {@code join: people as p on assignee_id key id} */
    static JoinNode join(TokenCursor cursor) {
        JoinNode join = new JoinNode();

        optionalColon(cursor);
        join.setTable(name(cursor));

        cursor.ensure(QueryToken.T_ON);
        join.setLocalColumn(name(cursor));

        cursor.ensure(QueryToken.T_KEY);
        join.setForeignColumn(name(cursor));

        return join;
    }

    /** {@code collection: request_tags on request_id value tag} */
    static CollectionNode collection(TokenCursor cursor) {
        CollectionNode collection = new CollectionNode();

        optionalColon(cursor);
        collection.setTable(name(cursor));

        cursor.ensure(QueryToken.T_ON);
        collection.setForeignKey(name(cursor));

        cursor.ensure(QueryToken.T_VALUE);
        collection.setValueColumn(name(cursor));

        return collection;
    }

    /**
     * One line of a mapping's {@code attributes { }} block — {@code key: request_key in column}.
     *
     * <p>⚠️ No type. It belongs to the structure, and a mapping repeating it would let two bindings of one
     * shape disagree about what a value is.</p>
     */
    static AttributeNode binding(TokenCursor cursor) {
        AttributeNode attribute = new AttributeNode();

        attribute.setName(path(cursor));

        cursor.ensure(BasicToken.T_COLON);
        attribute.setSource(qualified(cursor));

        cursor.ensure(BasicToken.T_IN);
        attribute.setAccess(access(cursor));

        return attribute;
    }

    /** Where a value is read from — the four shapes a store keeps one in. */
    static String access(TokenCursor cursor) {
        if (cursor.consumeIf(QueryToken.T_BAG)) {
            return "bag";
        }

        if (cursor.consumeIf(QueryToken.T_JOIN)) {
            return "join";
        }

        if (cursor.consumeIf(QueryToken.T_COLLECTION)) {
            return "collection";
        }

        cursor.ensure(QueryToken.T_COLUMN);

        return "column";
    }

    /**
     * Reads {@code name [as type[]] [: default]} — a parameter, wherever one is declared.
     *
     * <p>⚠️ Shared between a {@code function} and a {@code view} on purpose. The two declare parameters in
     * the same words, and a second reader would drift from this one the first time either grew a type.</p>
     *
     * <p>⚠️ A colon introduces a DEFAULT VALUE, which is core's meaning unchanged — the template engine's
     * macro reads it the same way. One punctuation mark meaning two things across two sibling dialects is
     * invisible until somebody copies a parameter list from one into the other.</p>
     */
    static ParameterDeclarationNode parameter(TokenCursor cursor, ParserContext context) {
        ParameterDeclarationNode parameter = new ParameterDeclarationNode();

        parameter.setName(cursor.ensure(BasicToken.T_IDENTIFIER).value());

        if (cursor.consumeIf(QueryToken.T_AS)) {
            parameter.setType(cursor.ensure(BasicToken.T_IDENTIFIER).value());

            if (cursor.consumeIf(BasicToken.T_OPEN_BRACKET)) {
                cursor.ensure(BasicToken.T_CLOSE_BRACKET);
                parameter.setCollection(true);
            }
        }

        if (cursor.consumeIf(BasicToken.T_COLON)) {
            parameter.setDefaultValue(
                    (Expression) context.getParser(ExpressionParser.class).parse(cursor, context));
        }

        return parameter;
    }

    /**
     * A comma-separated parameter list inside brackets, where one is written.
     *
     * @return what was declared, or an empty list when there are no brackets at all
     */
    static List<ParameterDeclarationNode> parameters(TokenCursor cursor, ParserContext context) {
        List<ParameterDeclarationNode> declared = new ArrayList<>();

        if (!cursor.consumeIf(BasicToken.T_OPEN_PAREN)) {
            return declared;
        }

        if (cursor.consumeIf(BasicToken.T_CLOSE_PAREN)) {
            return declared;
        }

        do {
            declared.add(parameter(cursor, context));
        } while (cursor.consumeIf(BasicToken.T_COMMA));

        cursor.ensure(BasicToken.T_CLOSE_PAREN);

        return declared;
    }

    /** Refuses anything that is not one of the words a type may be written with. */
    static String type(TokenCursor cursor, List<String> allowed) {
        Token written = cursor.ensure(BasicToken.T_IDENTIFIER);

        if (!allowed.contains(written.value())) {
            throw new QueryParseException(
                    "'%s' at line %d is not a kind of value; write one of %s".formatted(
                            written.value(), written.lineNumber(), String.join(", ", allowed)));
        }

        return written.value();
    }
}
