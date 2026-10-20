package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceNotation.Normalized;

import java.util.List;

/**
 * Reads the documents of a jMX text on the expression engine: {@link JmxExtension} is imported into the reading's
 * context beside jMS's, and its {@link JmxFileParser} reads the text.
 *
 * <p>No word of jMX is a lexer token. A document opens with a kind's word followed by a quoted title; a block is a
 * block's word at the start of a statement. Everything is recognised by its position, so {@code points}, {@code set},
 * {@code state} and {@code view} stay free as names inside {@code behaviour} and {@code run}, where jMS reads them.</p>
 */
public final class DocumentReader {

    private final Reading reading;

    /**
     * @param context a context of this reading's own: jMX's extension is imported into it
     */
    public DocumentReader(TokenCursor cursor, ParserContext context, Normalized normalized, String document) {
        this.reading = new Reading(cursor, context, normalized, document);
        context.getExtensionContainer().importExtension(new JmxExtension(reading));
    }

    /** Every document of the text, in order. */
    public List<ExperienceDocument> documents() {
        Node file = reading.context.getParser(JmxFileParser.class).parse(reading.cursor, reading.context);
        return file.getChildren().stream().map(node -> ((DocumentNode) node).document()).toList();
    }
}
