package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.FieldNode;
import org.jmouse.query.el.node.StructureNode;

/**
 * Parses {@code structure request { … }} — the shape, apart from anything that stores it.
 *
 * <pre>
 * structure request : base {
 *   key:      string
 *   priority: int, default: 3
 *   tags:     string[]
 * }
 * </pre>
 *
 * <h2>⚠️ Every key in this block is a USER's name, and that decides the grammar</h2>
 *
 * <p>So the language puts nothing of its own at that position. What it needs to say about the structure
 * as a whole — which one it extends — is written in the <strong>header</strong>, after a colon. A
 * {@code base:} line would be indistinguishable from an attribute somebody called {@code base}, and that
 * breaks in another product a year later rather than here.</p>
 *
 * <p>The same reasoning is why a field name is read permissively: {@code key}, {@code order} and
 * {@code from} are ordinary things to call a column, and every one of them is a word this grammar spends
 * elsewhere.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StructureParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(QueryToken.T_STRUCTURE);

        StructureNode structure = new StructureNode();

        structure.setName(Declarations.name(cursor));

        if (cursor.consumeIf(BasicToken.T_COLON)) {
            structure.setBaseStructure(Declarations.name(cursor));
        }

        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.skipBlankSpace(cursor);

            if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
                break;
            }

            structure.addField(field(cursor, context));
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        if (structure.getFields().isEmpty()) {
            throw new QueryParseException(
                    "structure '%s' declares no attributes".formatted(structure.getName()));
        }

        parent.add(structure);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(QueryToken.T_STRUCTURE);
    }

    private FieldNode field(TokenCursor cursor, ParserContext context) {
        FieldNode field = new FieldNode();

        field.setName(Declarations.path(cursor));

        cursor.ensure(BasicToken.T_COLON);
        field.setType(Declarations.type(cursor, FieldNode.TYPES));

        // ⚠️ `tags: string[]` — many values of one type, and the brackets say so where the type would
        // otherwise have to be spelled twice as `strings`.
        if (cursor.consumeIf(BasicToken.T_OPEN_BRACKET)) {
            cursor.ensure(BasicToken.T_CLOSE_BRACKET);
            field.setCollection(true);
        }

        // ⚠️ A default is part of what the SHAPE promises, so it is read here and nowhere else. Two
        // mappings of one structure disagreeing about one would be a difference visible from neither file.
        if (cursor.consumeIf(BasicToken.T_COMMA)) {
            cursor.ensure(QueryToken.T_DEFAULT);
            cursor.ensure(BasicToken.T_COLON);

            field.setDefaultValue((Expression) context.getParser(ExpressionParser.class).parse(cursor, context));
        }

        return field;
    }
}
