package org.jmouse.query.el.parser;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.FunctionNode;
import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.MappingNode;
import org.jmouse.query.el.node.SourceNode;
import org.jmouse.query.el.node.StructureNode;
import org.jmouse.query.el.node.ViewNode;

/**
 * Reads a whole {@code .jmq} document — every declaration in it, to the last one.
 *
 * <p>⚠️ <strong>This is the root parser, not one more registered alternative.</strong> An expression
 * parser reads one expression: asked for a document it returns the first declaration and stops, and a
 * second {@code view} below the first is not refused, not reported, simply absent. In a file whose
 * whole job is to say what to fetch, a declaration that quietly is not there is the worst failure
 * available — the query runs, returns rows, and nothing gives anybody a reason to look.</p>
 *
 * <p>So {@code supports} stays {@code false} and this is reached only as the entry point a caller hands
 * a document to.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryDocumentParser extends ExpressionParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        QueryDocumentNode document = new QueryDocumentNode();

        Documents.skipBlankSpace(cursor);

        while (hasMore(cursor)) {
            Documents.refuseReserved(cursor);

            int position = cursor.position();

            if (cursor.isCurrent(QueryToken.T_STRUCTURE)) {
                document.addStructure(
                        (StructureNode) context.getParser(StructureParser.class).parse(cursor, context));
            } else if (cursor.isCurrent(QueryToken.T_MAPPING)) {
                document.addMapping(
                        (MappingNode) context.getParser(MappingParser.class).parse(cursor, context));
            } else if (cursor.isCurrent(QueryToken.T_SOURCE)) {
                document.addSource((SourceNode) context.getParser(SourceParser.class).parse(cursor, context));
            } else if (cursor.isCurrent(QueryToken.T_VIEW)) {
                document.addView((ViewNode) context.getParser(ViewParser.class).parse(cursor, context));
            } else if (cursor.isCurrent(LanguageToken.T_FUNCTION)) {
                document.addFunction(
                        (FunctionNode) context.getParser(QueryFunctionParser.class).parse(cursor, context));
            } else {
                throw new QueryParseException(
                        ("'%s' at line %d is not a declaration; a query document holds 'structure', 'mapping', 'view' and "
                         + "'function' blocks")
                                .formatted(cursor.current().value(), cursor.current().lineNumber()));
            }

            // A parser that consumed nothing would spin here for ever. It cannot happen with the two
            // branches above, and it is the failure that would be hardest to diagnose if a third were
            // ever added carelessly.
            if (cursor.position() == position) {
                throw new QueryParseException(
                        "'%s' at line %d could not be read".formatted(
                                cursor.current().value(), cursor.current().lineNumber()));
            }

            Documents.skipBlankSpace(cursor);
        }

        parent.add(document);
    }

    /**
     * Whether anything but the end of the document is left.
     */
    private boolean hasMore(TokenCursor cursor) {
        return cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL);
    }
}
