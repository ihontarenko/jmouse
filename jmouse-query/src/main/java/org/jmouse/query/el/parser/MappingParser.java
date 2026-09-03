package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.MappingNode;

/**
 * Parses {@code mapping request { … }} and {@code mapping request:export { … }}.
 *
 * <pre>
 * mapping request {
 *   from: requests as r key id
 *   bag:  request_fields on request_id key field_code value text_value
 *
 *   attributes {
 *     key:      request_key  in column
 *     priority: "f-priority" in bag
 *   }
 * }
 * </pre>
 *
 * <h2>⚠️ Two kinds of key, in two blocks</h2>
 *
 * <p>The directives are the language's words and sit at the top level of the block. Every key inside
 * {@code attributes { }} is a user's name. Keeping them apart is what lets both use a colon: a word the
 * language gains next year can never shadow an attribute somebody called {@code limit} today.</p>
 *
 * <h2>⚠️ A mapping names no vendor</h2>
 *
 * <p>There is no {@code on mysql}. Quoting, paging and function names come from the dialect the
 * connection reports, and what kind of store this is is already said by the body — {@code from:} is table
 * talk. A file naming its vendor would state a second time what the connection already knows, and the two
 * would disagree.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MappingParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(QueryToken.T_MAPPING);

        MappingNode mapping = new MappingNode();

        mapping.setStructure(Declarations.name(cursor));

        // ⚠️ A colon after a name always means IDENTIFIER — here the second mapping of one structure,
        // exactly as it means the reference name of a view.
        if (cursor.consumeIf(BasicToken.T_COLON)) {
            mapping.setVariant(Declarations.name(cursor));
        }

        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.skipBlankSpace(cursor);

            if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
                break;
            }

            directive(cursor, mapping);
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        if (mapping.getTable() == null && mapping.getFile().isEmpty() && mapping.getRows().isEmpty()) {
            throw new QueryParseException(
                    ("mapping '%s' does not say where its rows are; write 'from: <table> as a key <column>', "
                     + "'from: $<binding>' or 'file: <name>'").formatted(mapping.getQualifiedName()));
        }

        parent.add(mapping);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(QueryToken.T_MAPPING);
    }

    private void directive(TokenCursor cursor, MappingNode mapping) {
        Token opening = cursor.peek();

        if (cursor.consumeIf(QueryToken.T_FROM_TABLE)) {
            from(cursor, mapping);
        } else if (cursor.consumeIf(QueryToken.T_BAG)) {
            mapping.setBag(Declarations.bag(cursor));
        } else if (cursor.consumeIf(QueryToken.T_JOIN)) {
            mapping.addJoin(Declarations.join(cursor));
        } else if (cursor.consumeIf(QueryToken.T_COLLECTION)) {
            mapping.addCollection(Declarations.collection(cursor));
        } else if (cursor.consumeIf(QueryToken.T_ATTRIBUTES)) {
            attributes(cursor, mapping);
        } else if (cursor.consumeIf(QueryToken.T_FILE)) {
            file(cursor, mapping);
        } else {
            throw QueryParseException.notAClause(
                    opening, "'from:', 'file:', 'bag:', 'join:', 'collection:' and 'attributes'");
        }
    }

    /**
     * {@code from: requests as r key id}, or {@code from: $rows}.
     *
     * <p>⚠️ The second form names a BINDING the runtime fills, and carries no alias and no key: a list of
     * maps has no table to alias and nothing to join on. Reading them in one place is what keeps a
     * mapping over rows and a mapping over tables the same kind of declaration.</p>
     */
    private void from(TokenCursor cursor, MappingNode mapping) {
        Declarations.optionalColon(cursor);

        if (cursor.consumeIf(BasicToken.T_DOLLAR)) {
            mapping.setRows(Declarations.name(cursor));

            return;
        }

        mapping.setTable(Declarations.name(cursor));

        cursor.ensure(QueryToken.T_AS);

        /* ⚠️ A keyword is a legal alias — the same rule `ClauseParser` states for a projection alias and
           `SourceParser` for the older spelling. A product whose table is called `labels` reasonably
           aliases it `label`, and the identifier rule refused that the day `label` became a word. */
        mapping.setAlias(Declarations.name(cursor));

        cursor.ensure(QueryToken.T_KEY);
        mapping.setKey(Declarations.name(cursor));
    }

    /**
     * {@code file: 'requests.csv', header: true, delimiter: ';'}
     *
     * <p>⚠️ The options are read by name and in any order, because there will be more of them and a
     * positional list is a list somebody has to count.</p>
     */
    private void file(TokenCursor cursor, MappingNode mapping) {
        Declarations.optionalColon(cursor);

        mapping.setFile(Declarations.name(cursor));

        while (cursor.consumeIf(BasicToken.T_COMMA)) {
            String option = Declarations.name(cursor);

            cursor.ensure(BasicToken.T_COLON);

            switch (option) {
                case "header" -> mapping.setHeader(flag(cursor));
                case "delimiter" -> mapping.setDelimiter(Declarations.name(cursor));
                default -> throw new QueryParseException(
                        ("'%s' is not something a file mapping says; it says 'header' and 'delimiter'")
                                .formatted(option));
            }
        }
    }

    /**
     * ⚠️ {@code true} and {@code false} are the language's own literals, not identifiers, so the ordinary
     * name reader refuses them. Read for what they are.
     */
    private boolean flag(TokenCursor cursor) {
        if (cursor.consumeIf(BasicToken.T_TRUE)) {
            return true;
        }

        cursor.ensure(BasicToken.T_FALSE);

        return false;
    }

    /**
     * {@code attributes { … }}, or the shorthand {@code attributes: identity}.
     *
     * <p>⚠️ The shorthand is not a convenience for the file — it is what stops a mapping over rows already
     * keyed by attribute name being fifteen lines that all say the same thing, and therefore fifteen lines
     * that can drift from the structure one at a time.</p>
     */
    private void attributes(TokenCursor cursor, MappingNode mapping) {
        if (cursor.consumeIf(BasicToken.T_COLON)) {
            cursor.ensure(QueryToken.T_IDENTITY);
            mapping.setIdentity(true);

            return;
        }

        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.skipBlankSpace(cursor);

            if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
                break;
            }

            mapping.addAttribute(Declarations.binding(cursor));
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);
    }
}
