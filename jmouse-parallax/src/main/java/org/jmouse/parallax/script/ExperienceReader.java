package org.jmouse.parallax.script;

import org.jmouse.el.StringSource;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.SyntaxErrorException;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.reader.DocumentReader;
import org.jmouse.script.el.ScriptExtension;
import org.jmouse.script.el.lexer.ScriptRecognizer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code .jmx} text into its documents.
 *
 * <p>jMX is read with jMS's lexer and nothing added to it — no word of jMX is a token — so a {@code behaviour} or a
 * {@code run} body is jMS exactly, parsed by jMS's own statement reader. Everything around those bodies is read by
 * {@link DocumentReader}, by position.</p>
 *
 * <p>Reading checks structure only. Whether a function an expression calls exists, or a part's component is in the
 * catalogue, is decided where the expression is compiled and the reference resolved.</p>
 */
public final class ExperienceReader {

    /**
     * Documents already read, by name and text: reading is a pure function of the text, and a circuit reads the same
     * component models every time it is built — on every run, every edit and every moved slider. Bounded, least recently
     * used first out.
     */
    private static final int READ_MEMORY = 512;
    private static final Map<String, ExperienceDocument> READ = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ExperienceDocument> eldest) {
                    return size() > READ_MEMORY;
                }
            });

    private ExperienceReader() {
    }

    /**
     * @param source       the text of a {@code .jmx} file
     * @param documentName what the text is called in a refusal: a file name, a catalogue path
     * @return every document the text holds, in order
     * @throws ExperienceParseException when the text is not jMX, naming the line and column
     */
    public static List<ExperienceDocument> read(String source, String documentName) {
        ExperienceNotation.Normalized normalized = ExperienceNotation.normalize(source);
        Lexer lexer = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ScriptRecognizer()));
        TokenCursor cursor;
        try {
            cursor = lexer.tokenize(new StringSource(documentName, normalized.source()));
        } catch (SyntaxErrorException failure) {
            throw new ExperienceParseException(documentName, 0, 0, failure.getMessage());
        }
        return new DocumentReader(cursor, context(), normalized, documentName).documents();
    }

    /** The one document a text holds; refuses a text with several. Remembered, since the text decides the document. */
    public static ExperienceDocument readOne(String source, String documentName) {
        String key = documentName + '\u0000' + source;
        ExperienceDocument known = READ.get(key);
        if (known != null) {
            return known;
        }
        ExperienceDocument document = readFresh(source, documentName);
        READ.put(key, document);
        return document;
    }

    private static ExperienceDocument readFresh(String source, String documentName) {
        List<ExperienceDocument> documents = read(source, documentName);
        if (documents.size() != 1) {
            throw new ExperienceParseException(documentName, documents.get(1).line(), 0,
                    "this text holds " + documents.size() + " documents where one was expected; one object is one file");
        }
        return documents.getFirst();
    }

    private static ParserContext context() {
        StandardExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new ScriptExtension());
        // A behaviour's `let delay : time` names jMX's quantities, as an equation's `let` does.
        extensions.importExtension(new ElectricalTypes());
        return new DefaultParserContext(extensions);
    }
}
