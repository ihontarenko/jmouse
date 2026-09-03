package org.jmouse.validator.el;

import org.jmouse.el.StringSource;
import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.validator.el.lexer.JmvRecognizer;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.parser.JmvSyntaxException;
import org.jmouse.validator.el.parser.ValidationDocumentParser;

/**
 * Reads {@code .jmv} source into a document. 📥
 *
 * <h2>⚠️ Files are read once, up front — never on the validating path</h2>
 *
 * <p>A reader called while a record is being validated would put a lexer and a parser behind a cache
 * miss and — far worse — would turn a malformed file into a failure in front of whoever submitted the
 * record, rather than a startup that refuses to happen. Everything this class does happens before the
 * first record is judged, or it is being used wrongly.</p>
 *
 * <h2>⚠️ Two vocabularies, and they are deliberately not the same one</h2>
 *
 * <p>What is built here reads the <em>file</em>: the engine's parsers plus this language's. What a
 * guard or a message may then <em>use</em> is a narrower question answered somewhere else entirely,
 * by whatever {@code ExpressionLanguage} the runtime compiles a sliced expression with. Keeping them
 * apart is what lets a product widen one without widening the other.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmvReader {

    /**
     * The lexer this language is read with.
     *
     * <p>⚠️ The splitter is the expression language's own — a {@code .jmv} file is made of the same
     * characters an expression is, and cutting it up differently would be a second answer to a question
     * already answered. Only the <em>recognizer</em> differs, because only the keywords do.</p>
     */
    private static final Lexer LEXER = new DefaultLexer(
            new DefaultTokenizer(new ExpressionSplitter(), new JmvRecognizer()));

    private final ParserContext context;

    public JmvReader() {
        this(defaultContext());
    }

    public JmvReader(ParserContext context) {
        this.context = context;
    }

    /**
     * Parses one file's text into a document.
     *
     * @param source what the file says
     * @param file   what to call it in a failure
     * @return the document
     * @throws JmvSyntaxException naming the file and the line
     */
    public ValidationDocumentNode parse(String source, String file) {
        try {
            TokenCursor cursor    = LEXER.tokenize(new StringSource(file, source));
            Node        container = BasicNode.forToken(cursor.current());

            context.getParser(ValidationDocumentParser.class).parse(cursor, container, context);

            return (ValidationDocumentNode) container.getFirst();
        } catch (JmvSyntaxException failure) {
            // ⚠️ The parser reads a cursor and has no idea what it was opened from. Rather than thread a
            // file name through every parser so one message can carry it, the name is stamped here.
            throw failure.at(file);
        }
    }

    /**
     * The engine's vocabulary plus this language's.
     *
     * @return a context every jMV parser has been registered with
     */
    private static ParserContext defaultContext() {
        ExtensionContainer extensions = new StandardExtensionContainer();

        extensions.importExtension(new CoreExtension());
        extensions.importExtension(new JmvExtension());

        return new DefaultParserContext(extensions);
    }
}
