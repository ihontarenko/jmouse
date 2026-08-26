package org.jmouse.mapper.el;

import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.mapper.el.lexer.JmmRecognizer;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.parser.JmmParser;
import org.jmouse.mapper.el.parser.JmmSyntaxException;

/**
 * Reads {@code .jmm} source into rules the mapping engine can use. 📥
 *
 * <h2>⚠️ Files are read once, up front — never on the mapping path</h2>
 *
 * <p>{@link JmmRuleSource} is consulted while objects are being mapped. A reader called from there
 * would put a lexer and a parser behind a cache miss, and — far worse — would turn a malformed file
 * into a mapping that fails in production rather than a startup that refuses to happen. Everything this
 * class does happens before the first object is mapped, or it is being used wrongly.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmReader {

    /**
     * The lexer this language is read with.
     *
     * <p>⚠️ The splitter is the expression language's own — a {@code .jmm} file is made of the same
     * characters an expression is, and cutting it up differently would be a second answer to a question
     * already answered. Only the <em>recognizer</em> differs, because only the keywords do.</p>
     */
    private static final Lexer LEXER = new DefaultLexer(
            new DefaultTokenizer(new ExpressionSplitter(), new JmmRecognizer()));

    private final JmmBinder binder;

    public JmmReader() {
        this(new JmmBinder());
    }

    public JmmReader(JmmBinder binder) {
        this.binder = binder;
    }

    /**
     * Parses one file's text into a document.
     *
     * @param source what the file says
     * @param file   what to call it in a failure
     * @return the document
     * @throws JmmSyntaxException naming the file and the line
     */
    public MappingDocumentNode parse(String source, String file) {
        try {
            TokenCursor cursor = LEXER.tokenize(new StringSource(file, source));

            return JmmParser.parse(cursor);
        } catch (JmmSyntaxException failure) {
            // ⚠️ The parser reads a cursor and has no idea what it was opened from. Rather than thread
            // a file name through every parser so one message can carry it, the name is stamped here.
            throw failure.at(file);
        }
    }

    /**
     * Reads one file and registers everything it declares.
     *
     * @param source what the file says
     * @param file   what to call it in a failure
     * @param into   where the rules are registered
     * @return {@code into}, so several files can be read in a chain
     */
    public JmmRuleSource read(String source, String file, JmmRuleSource into) {
        return read(parse(source, file), file, into);
    }

    /**
     * Registers everything a document already parsed declares.
     *
     * <p>⚠️ Exposed because a loader has to look at a document <em>before</em> binding it — to check
     * that no other file already claims its targets, and refuse before half of this one's rules have
     * reached the source. Without this overload it parsed the text once to look and the reader parsed it
     * again to bind: two trees, two full lex-parse cycles, for one file.</p>
     *
     * @param document the parsed file
     * @param file     what to call it in a failure
     * @param into     where the rules are registered
     * @return {@code into}, so several documents can be read in a chain
     */
    public JmmRuleSource read(MappingDocumentNode document, String file, JmmRuleSource into) {
        try {
            binder.bind(document, into);
        } catch (JmmSyntaxException failure) {
            throw failure.source() == null ? failure.at(file) : failure;
        }

        return into;
    }

    /**
     * Reads one file into a fresh rule source.
     *
     * @param source what the file says
     * @param file   what to call it in a failure
     * @return a source holding what it declared
     */
    public JmmRuleSource read(String source, String file) {
        return read(source, file, new JmmRuleSource());
    }
}
