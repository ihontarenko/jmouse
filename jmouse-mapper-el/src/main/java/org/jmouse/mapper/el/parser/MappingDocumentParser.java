package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;

/**
 * Reads a whole {@code .jmm} file — the trivia around the block as well as the block.
 *
 * <p>⚠️ A second class rather than two more lines in {@link MappingParser}, because leading comments,
 * blank lines and whatever follows the closing brace are a question about a <em>file</em>.
 * {@link org.jmouse.el.language.parser.AbstractBlockParser} knows nothing about files and should not.
 * jMP and jMV split the same way.</p>
 *
 * <p>⚠️ Its {@code supports} stays {@code false}. This is the root a reader hands a whole file to, not
 * one more shape a statement might be, and offering it to dispatch would let a document nest inside
 * another — a file with two identities and nothing saying which is the outer one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MappingDocumentParser extends ExpressionParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Separators.skip(cursor);

        parent.add(context.getParser(MappingParser.class).parse(cursor, context));

        Separators.skip(cursor);

        // ⚠️ Refused rather than ignored. A declaration after the closing brace is somebody's rule, and
        // dropping it silently means a file that maps less than its author believes it does.
        if (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL)) {
            throw new JmmSyntaxException(cursor,
                    ("a file declaring 'mapping' declares nothing beside it, and '%s' is outside the "
                     + "block; move it inside").formatted(cursor.current().value()));
        }
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return false;
    }
}
