package org.jmouse.validator.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.validator.el.lexer.JmvToken;
import org.jmouse.validator.el.node.CheckBlockNode;
import org.jmouse.validator.el.node.ValidationDocumentNode;

/**
 * Reads the {@code validation "name" { … }} block — the whole of a file, once its trivia is past.
 *
 * <p>⚠️ The <em>root</em> is {@link ValidationDocumentParser}, not this. Splitting them is what jMP
 * does and for the same reason: leading comments, blank lines and whatever follows the closing brace
 * are a question about a <em>file</em>, and {@link org.jmouse.el.language.parser.AbstractBlockParser}
 * quite rightly knows nothing about files — it ensures its keyword under the cursor and would refuse a
 * document whose first line is a comment.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ValidationParser extends JmvBlockParser<ValidationDocumentNode, JmvToken> {

    /**
     * Builds the document, reading its name.
     *
     * <p>The base class has consumed {@code validation}, so the quoted name is what stands under the
     * cursor and the opening brace is what follows — which is where the body parser expects to start.</p>
     */
    @Override
    protected ValidationDocumentNode createNode(TokenCursor cursor, ParserContext context) {
        ValidationDocumentNode document = new ValidationDocumentNode();

        document.setName(SourceReading.literal(cursor.ensure(BasicToken.T_STRING)));

        return document;
    }

    @Override
    protected JmvToken token() {
        return JmvToken.T_VALIDATION;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.isCurrent(JmvToken.T_VALIDATION);
    }

    /**
     * Reads the body, then refuses a second gate.
     *
     * <p>⚠️ Two blocks that each stop the whole document are one block written in two places, and the
     * order they would run in is the order somebody happened to type them. Checked once the body is
     * read rather than as each statement arrives, because the statements arrive through dispatch and a
     * parser reaching into that would be doing the framework's job badly.</p>
     */
    @Override
    protected void parseBody(TokenCursor cursor, ValidationDocumentNode node, ParserContext context) {
        super.parseBody(cursor, node, context);

        if (node.blocksOf(CheckBlockNode.Kind.GATE).size() > 1) {
            throw new JmvSyntaxException(cursor,
                    "a document has one 'gate' — a second one is the same block written twice, and "
                    + "nothing says which of them runs first");
        }
    }
}
