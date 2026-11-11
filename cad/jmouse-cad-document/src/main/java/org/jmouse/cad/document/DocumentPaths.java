package org.jmouse.cad.document;

import org.jmouse.el.grammar.QuotedText;

import org.jmouse.el.grammar.GrammarRefusal;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.svg.path.parser.PathParseException;
import org.jmouse.svg.path.parser.PathParser;

/**
 * Reads path data a document writes in quotes into its tree, by jmouse-svg-path's parser. A refusal of the path data is
 * a refusal of the document, at the column of the document's line where it stands.
 */
public final class DocumentPaths {

    private static final PathParser PARSER = new PathParser();

    private DocumentPaths() {
    }

    /** The path data of a text in quotes the cursor has just read. */
    public static PathTextNode read(TokenCursor cursor, Token text) {
        DocumentSource source = DocumentSource.of(cursor);
        try {
            return new PathTextNode(PARSER.parse(source.getName(), QuotedText.of(text)));
        } catch (PathParseException refusal) {
            throw new GrammarRefusal(source.getName(), text.lineNumber(), source.column(text) + 1 + refusal.offset(),
                    "in the path data: " + refusal.detail());
        }
    }
}
