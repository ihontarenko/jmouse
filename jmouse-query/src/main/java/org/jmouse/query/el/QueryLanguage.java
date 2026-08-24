package org.jmouse.query.el;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.query.el.lexer.QueryLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.query.el.lexer.QueryRecognizer;
import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.parser.OrderParser;
import org.jmouse.query.el.parser.QueryDocumentParser;

/**
 * jMQ — reading a query, in either of the two shapes one comes in.
 *
 * <h2>Two entry points, one parser</h2>
 *
 * <pre>{@code
 * QueryLanguage jmq = new QueryLanguage();
 *
 * // an expression — a URL parameter, a config value, an annotation, a CLI argument
 * Expression filter = jmq.expression("entry[quantity] | int < 5");
 *
 * // a document — a saved view, a file of reusable functions
 * QueryDocumentNode document = jmq.document("""
 *         view "Мої косарки" on inventory {
 *           where   entry[component_name] is contains("кос")
 *           order   entry[quantity] | int asc
 *           columns component_name, quantity, location
 *         }
 *         """);
 * }</pre>
 *
 * <p>⚠️ <strong>They share a parser, and that is the design rather than an implementation detail.</strong>
 * The {@code where} inside a document is not "a string that is later parsed as an expression" — it
 * <em>is</em> the expression, read by the same parser in the same pass. So a filter written into a URL
 * and the identical filter written into a file cannot come to mean different things: there is no second
 * place for precedence, quoting or operator behaviour to differ.</p>
 *
 * <h2>⚠️ This class knows nothing about SQL</h2>
 *
 * <p>Nor about JDBC, a dialect, a table or a column. Translating a parsed query into something a data
 * source can run lives in a separate module, so that anything which merely needs to <em>read</em>,
 * <em>write</em> or <em>reason about</em> a query — a builder, a linter, an editor, a validator — can
 * depend on this one without acquiring a database.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryLanguage {

    private final ExpressionLanguage documents;
    private final ExpressionLanguage expressions;
    private final ExpressionLanguage orders;

    public QueryLanguage() {
        this(defaultExtensions());
    }

    /**
     * ⚠️ Two languages over <strong>one</strong> extension container, and the sharing is the point.
     *
     * <p>They differ only in which parser they start at — the document root, or the ordinary expression
     * parser. Everything that decides what an expression <em>means</em> — the operators and their
     * precedence, the tests, the converter filters — is the same object in both. Building them from two
     * containers would produce two languages that agree today and drift the first time one of them is
     * given something the other is not.</p>
     *
     * <p>The container's vocabulary is
     * {@link org.jmouse.query.el.dialect.QueryDialect}'s, which {@link QueryExtension} extends by adding
     * three tag parsers and nothing else. So the confinement applies to a condition however it arrived —
     * out of a URL, out of a file — without either path being told about it separately.</p>
     *
     * @param extensions the vocabulary both entry points read against
     */
    public QueryLanguage(ExtensionContainer extensions) {
        this.documents = new ExpressionLanguage(extensions, lexer(), QueryDocumentParser.class);
        this.expressions = new ExpressionLanguage(extensions, lexer(), ExpressionParser.class);
        this.orders = new ExpressionLanguage(extensions, lexer(), OrderParser.class);
    }

    /**
     * Reads a whole {@code .jmq} document.
     *
     * @param source the document's text
     * @return the views and functions it declares
     * @throws QueryParseException when the text is not a query document
     */
    public QueryDocumentNode document(String source) {
        if (documents.compile(source) instanceof QueryDocumentNode document) {
            return document;
        }

        throw new QueryParseException(
                "this is not a query document; expected a 'view' or 'function' declaration");
    }

    /**
     * Whether this text is a whole document, or one bare expression.
     *
     * <h2>⚠️ Asked rather than remembered</h2>
     *
     * <p>A saved query is kept as text, and the two shapes live side by side: a board's filter is one
     * condition, a report is a {@code view} block with its own columns and order. A column recording
     * which of the two it is would be a second statement of something the text already says — and the
     * two would disagree the first time somebody edited a filter into a view.</p>
     *
     * <p>So the question is put to the parser that would have to read it. That is exact by construction:
     * this is a document precisely when the document parser can read it as one.</p>
     *
     * <p>⚠️ A refusal here means only <em>"not a document"</em>, and is deliberately not re-thrown. Text
     * that is neither a document nor an expression fails on the next step with the expression parser's
     * message — which is the one somebody composing a filter needs to read.</p>
     *
     * @param source the text
     * @return {@code true} when it declares sources, views or functions
     */
    public boolean isDocument(String source) {
        try {
            QueryDocumentNode document = document(source);

            return !document.getViews().isEmpty()
                   || !document.getFunctions().isEmpty()
                   || !document.getSources().isEmpty();
        } catch (QueryParseException notADocument) {
            return false;
        }
    }

    /**
     * Reads one expression — a filter, a sort key, a projection.
     *
     * @param source the expression's text
     * @return the parsed expression
     */
    public Expression expression(String source) {
        return expressions.compile(source);
    }

    /**
     * Reads a bare {@code order} clause — the sort keys, without the keyword.
     *
     * <pre>{@code
     * OrderNode sort = jmq.order("entry[quantity] | int desc, created asc");
     * }</pre>
     *
     * <h2>⚠️ Its own entry point, so a URL never assembles a document</h2>
     *
     * <p>A sort is not one expression — it is keys and directions — so the tempting implementation wraps
     * it in {@code view "…" on x { order … }} and parses that. ⚠️ That is <strong>building a query by
     * concatenating a string out of a URL</strong>: a sort containing a brace closes the block early and
     * adds clauses nobody asked for. It reaches no data the schema does not describe, but it lets a
     * caller restructure the query, and "they probably will not type a brace" is not a safety property.</p>
     *
     * <p>Read here, a brace is a syntax error with a position — which is what it is.</p>
     *
     * @param source the sort keys
     * @return the clause
     * @throws QueryParseException when it is not a sort
     */
    public OrderNode order(String source) {
        if (orders.compile(source) instanceof OrderNode clause) {
            return clause;
        }

        throw new QueryParseException(
                "this is not a sort; write one or more keys, as in 'entry[quantity] | int desc'");
    }

    /**
     * The expression half, for something that needs to <em>evaluate</em> rather than compile — an
     * in-memory backend, a preview, a validation pass.
     *
     * <p>⚠️ The <strong>same</strong> extension container the document half uses, so what an expression
     * means cannot differ between running it and compiling it. That is the whole property two backends
     * exist to protect.</p>
     */
    public ExpressionLanguage expressionLanguage() {
        return expressions;
    }

    /**
     * Reads a document and writes it straight back out.
     *
     * <p>The result is the same query in the language's own spelling — canonical clause order, one
     * clause per line, names quoted exactly where the lexer needs them. Two properties make it worth
     * having: a builder can save what a person composed as text somebody else can read, and rewriting
     * an already-rewritten document changes nothing, which is what makes the output safe to diff.</p>
     *
     * <p>⚠️ <strong>Comments do not survive.</strong> They are not part of what the document declares
     * and the parser does not keep them, so a rewrite is a rendering of the query rather than an edit
     * of the text. Anything that rewrites a document a person may have commented has to say so.</p>
     *
     * @param source the document's text
     * @return the same query, normalised
     * @throws QueryParseException when the text is not a query document
     */
    public String rewrite(String source) {
        return document(source).toSource();
    }

    /**
     * ⚠️ QueryLexer, not DefaultLexer: a newline means nothing in this language, and the shared lexer
     * emits a token for one — which ended a condition wrapped over two lines. See QueryLexer.
     */
    private static QueryLexer lexer() {
        return new QueryLexer(new DefaultTokenizer(new ExpressionSplitter(), new QueryRecognizer()));
    }

    private static ExtensionContainer defaultExtensions() {
        StandardExtensionContainer container = new StandardExtensionContainer();

        container.importExtension(new QueryExtension());

        return container;
    }
}
