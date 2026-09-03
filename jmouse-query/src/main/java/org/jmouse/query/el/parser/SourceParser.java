package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.AttributeNode;
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

        source.setName(Declarations.name(cursor));

        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.refuseReserved(cursor);

            Token opening = cursor.peek();

            if (cursor.consumeIf(QueryToken.T_FROM_TABLE)) {
                from(cursor, source);
            } else if (cursor.consumeIf(QueryToken.T_BAG)) {
                source.setBag(Declarations.bag(cursor));
            } else if (cursor.consumeIf(QueryToken.T_JOIN)) {
                source.addJoin(Declarations.join(cursor));
            } else if (cursor.consumeIf(QueryToken.T_COLLECTION)) {
                source.addCollection(Declarations.collection(cursor));
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
        source.setTable(Declarations.name(cursor));

        cursor.ensure(QueryToken.T_AS);

        /*
         * ⚠️ **A keyword is a legal alias**, read through the same permissive rule every other name in
         * this grammar uses. `ClauseParser` already says this about a projection alias; the two source
         * spellings never got it, and the identifier rule here was a latent trap rather than a decision.
         *
         * It sprang the day `label` became a word: Innoventa declares `from label_templates as label`,
         * which had parsed happily for as long as the file existed and then failed at BOOT with
         * *expected T_IDENTIFIER, encountered T_LABEL*. A grammar that dictates what a product may call
         * its own tables is a grammar that cannot grow a keyword — which is exactly what the note above
         * `T_LABEL` warns about, arriving from the other direction.
         *
         * Unambiguous for the usual reason: a name only appears where a name can appear, and the token
         * after an alias is always `key`.
         */
        source.setAlias(Declarations.name(cursor));

        cursor.ensure(QueryToken.T_KEY);
        source.setKey(Declarations.name(cursor));
    }

    /*
     * ⚠️ **`bag`, `join`, `collection` and every name helper are read by `Declarations`, not here.**
     *
     * This parser reads the older `source { }` spelling and `MappingParser` reads the current
     * `structure` / `mapping` pair — and the two declare those clauses in exactly the same words. They
     * used to be two copies, which is the thing `Declarations` was created to prevent; its own class
     * comment predicts the failure precisely: *a second implementation would agree with the first until
     * somebody fixed a bug in one of them*.
     *
     * That is not hypothetical. Teaching the bag line its `matching` clause landed in one copy and not
     * the other, so a source declared in the older spelling correlated against the wrong column and
     * matched nothing — with no error anywhere. The copies are gone.
     */

    /** {@code attribute entry[quantity] from "f-quantity" unknown in bag[, label: "How many"]} */
    private AttributeNode attribute(TokenCursor cursor) {
        AttributeNode attribute = new AttributeNode();

        attribute.setName(Declarations.path(cursor));

        cursor.ensure(QueryToken.T_FROM_TABLE);
        attribute.setSource(Declarations.qualified(cursor));

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

        /*
         * ⚠️ **`, label: "…"` — the same comma-clause the modern spelling's field line uses.**
         *
         * This older spelling states the shape and the binding on one line, so the label arrives here;
         * `SourceNode.toStructure()` then moves it onto the field, where it belongs. Writing it as a
         * trailing comma-clause rather than inventing an adjacency keeps ONE grammar for trailing facts
         * across both spellings — and the next fact after this one has no choice to make.
         */
        if (cursor.consumeIf(BasicToken.T_COMMA)) {
            cursor.ensure(QueryToken.T_LABEL);
            cursor.ensure(BasicToken.T_COLON);

            /* ⚠️ Through `word`, so the quotes come off. Reading the token's raw value instead would
               store `"Lines with no source"` — quotes and all — and a builder would print them. */
            attribute.setLabel(Declarations.text(cursor));
        }

        return attribute;
    }
}
