package org.jmouse.query.el.parser;

import org.jmouse.core.MimeParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.AttributeNode;
import org.jmouse.query.el.node.BagNode;
import org.jmouse.query.el.node.CollectionNode;
import org.jmouse.query.el.node.JoinNode;
import org.jmouse.query.el.node.SourceNode;

import java.util.List;

/**
 * Parses {@code source name { from … bag … attribute … }}.
 *
 * <pre>
 * source inventory {
 *   from      form_entries as e key id
 *   bag       field_entries on form_entry_id key field_id value text_value
 *   attribute entry[name]     from "f-component-name" text     in bag
 *   attribute entry[quantity] from "f-quantity"       unknown  in bag
 *   attribute created         from created_at         temporal in column
 * }
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SourceParser extends AbstractParser {

    /** What a type may be written as — checked here so the message names the choice, not a class. */
    private static final List<String> TYPES = List.of("text", "number", "boolean", "temporal", "unknown");

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(QueryToken.T_SOURCE);

        SourceNode source = new SourceNode();

        source.setName(name(cursor));

        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.refuseReserved(cursor);

            Token opening = cursor.peek();

            if (cursor.consumeIf(QueryToken.T_FROM_TABLE)) {
                from(cursor, source);
            } else if (cursor.consumeIf(QueryToken.T_BAG)) {
                source.setBag(bag(cursor));
            } else if (cursor.consumeIf(QueryToken.T_JOIN)) {
                source.addJoin(join(cursor));
            } else if (cursor.consumeIf(QueryToken.T_COLLECTION)) {
                source.addCollection(collection(cursor));
            } else if (cursor.consumeIf(QueryToken.T_ATTRIBUTE)) {
                source.addAttribute(attribute(cursor));
            } else {
                throw QueryParseException.notAClause(
                        opening, "'from', 'bag', 'join', 'collection' and 'attribute'");
            }
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        // ⚠️ Refused here rather than left to fail later. A source with no `from` describes nothing, and
        // the message a compiler could give — "no table" — would arrive far from the file that omitted it.
        if (source.getTable() == null) {
            throw new QueryParseException(
                    "source '%s' does not say where its rows are; add a 'from' line".formatted(source.getName()));
        }

        parent.add(source);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(QueryToken.T_SOURCE);
    }

    /** {@code from form_entries as e key id} */
    private void from(TokenCursor cursor, SourceNode source) {
        source.setTable(name(cursor));

        cursor.ensure(QueryToken.T_AS);
        source.setAlias(cursor.ensure(BasicToken.T_IDENTIFIER).value());

        cursor.ensure(QueryToken.T_KEY);
        source.setKey(name(cursor));
    }

    /** {@code bag field_entries on form_entry_id key field_id value text_value} */
    private BagNode bag(TokenCursor cursor) {
        BagNode bag = new BagNode();

        bag.setTable(name(cursor));

        cursor.ensure(QueryToken.T_ON);
        bag.setForeignKey(name(cursor));

        cursor.ensure(QueryToken.T_KEY);
        bag.setKeyColumn(name(cursor));

        cursor.ensure(QueryToken.T_VALUE);
        bag.setValueColumn(name(cursor));

        return bag;
    }

    /** {@code join statuses on status_id key id} */
    private JoinNode join(TokenCursor cursor) {
        JoinNode join = new JoinNode();

        join.setTable(name(cursor));

        cursor.ensure(QueryToken.T_ON);
        join.setLocalColumn(name(cursor));

        cursor.ensure(QueryToken.T_KEY);
        join.setForeignColumn(name(cursor));

        return join;
    }

    /** {@code collection labels on issue_id value name} */
    private CollectionNode collection(TokenCursor cursor) {
        CollectionNode collection = new CollectionNode();

        collection.setTable(name(cursor));

        cursor.ensure(QueryToken.T_ON);
        collection.setForeignKey(name(cursor));

        cursor.ensure(QueryToken.T_VALUE);
        collection.setValueColumn(name(cursor));

        return collection;
    }

    /** {@code attribute entry[quantity] from "f-quantity" unknown in bag} */
    private AttributeNode attribute(TokenCursor cursor) {
        AttributeNode attribute = new AttributeNode();

        attribute.setName(path(cursor));

        cursor.ensure(QueryToken.T_FROM_TABLE);
        attribute.setSource(qualified(cursor));

        Token typed = cursor.ensure(BasicToken.T_IDENTIFIER);

        if (!TYPES.contains(typed.value())) {
            throw new QueryParseException(
                    "'%s' at line %d is not a kind of value; write one of %s".formatted(
                            typed.value(), typed.lineNumber(), String.join(", ", TYPES)));
        }

        attribute.setType(typed.value());

        cursor.ensure(BasicToken.T_IN);

        if (cursor.consumeIf(QueryToken.T_BAG)) {
            attribute.setAccess("bag");
        } else if (cursor.consumeIf(QueryToken.T_JOIN)) {
            attribute.setAccess("join");
        } else if (cursor.consumeIf(QueryToken.T_COLLECTION)) {
            attribute.setAccess("collection");
        } else {
            cursor.ensure(QueryToken.T_COLUMN);
            attribute.setAccess("column");
        }

        return attribute;
    }

    /**
     * A name a query writes — {@code entry[quantity]}, {@code issue.points}, {@code created}.
     *
     * <p>⚠️ Read as raw text rather than parsed as a property, because it is a <em>key</em> here and not
     * an expression: the schema is looked up by exactly the characters a query will write. Parsing and
     * re-rendering it would introduce a second spelling of the same thing, and the two would drift.</p>
     */
    private String path(TokenCursor cursor) {
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
     * One segment of a name — and ⚠️ <strong>a keyword counts as a word here</strong>.
     *
     * <p>{@code issue.key}, {@code entry[value]}, {@code order.from} are perfectly ordinary things for a
     * product to call its data, and every one of them collides with a word this grammar spends
     * elsewhere. Refusing them would mean a language that dictates what a database may name its columns,
     * which no product would accept — and the collision would be discovered by the first product whose
     * schema happened to include one.</p>
     *
     * <p>It is unambiguous because a name only appears where a name can appear: a path segment follows a
     * {@code .} or a {@code [}, and the clause keyword that ends the path is the token after it. This is
     * the same reason SQL lets a quoted identifier be {@code order}.</p>
     */
    private String word(TokenCursor cursor) {
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
    private String name(TokenCursor cursor) {
        return word(cursor);
    }

    /**
     * A stored name that may name its table too — {@code statuses.category}.
     *
     * <p>⚠️ Only for what the STORE calls something, never for what a query writes. A joined attribute
     * has to say which of the source's joins it reads, and the last dot is what tells {@code statuses}
     * from {@code types}. Unambiguous because the token after a stored name is always a type word, which
     * cannot follow a dot.</p>
     */
    private String qualified(TokenCursor cursor) {
        StringBuilder written = new StringBuilder(word(cursor));

        while (cursor.isCurrent(BasicToken.T_DOT)) {
            cursor.ensure(BasicToken.T_DOT);
            written.append('.').append(word(cursor));
        }

        return written.toString();
    }
}
