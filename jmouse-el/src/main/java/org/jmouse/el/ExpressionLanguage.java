package org.jmouse.el;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.renderable.Cache;

import java.util.Collections;
import java.util.Map;

/**
 * 🔎 Engine for parsing, compiling, caching, and evaluating expressions.
 * <p>
 * Manages a lexer, parser, and cache for {@link ExpressionNode} instances, as well as
 * an {@link ExtensionContainer} for custom functions, operators, and filters.
 * </p>
 */
public class ExpressionLanguage {

    private final ParserContext                    context;
    private final Lexer                            lexer;
    private final ExpressionParser                 parser;
    private final Cache<Cache.Key, ExpressionNode> cache;
    private final ExtensionContainer               extensions;

    /**
     * Constructs a new ExpressionLanguage with default extensions, lexer, parser context, and cache.
     * <p>
     * Automatically imports the core expression-language extension ({@link CoreExtension}).
     * </p>
     */
    public ExpressionLanguage() {
        this.extensions = new StandardExtensionContainer() {{
            importExtension(new CoreExtension());
        }};
        this.lexer = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()));
        this.context = new DefaultParserContext(getExtensions());
        this.cache = Cache.memory();
        this.parser = (ExpressionParser) context.getParser(ExpressionParser.class);
    }

    /**
     * Creates a fresh {@link EvaluationContext} for expression evaluation,
     * initialized with this engine's extensions.
     *
     * @return a new {@link EvaluationContext}
     */
    public EvaluationContext newContext() {
        EvaluationContext ctx = new DefaultEvaluationContext();
        ctx.setExtensions(extensions);
        return ctx;
    }

    /**
     * Returns the extensions container used by this engine.
     *
     * @return the {@link ExtensionContainer}
     */
    public ExtensionContainer getExtensions() {
        return extensions;
    }

    /**
     * Compiles the given expression string into an AST ({@link ExpressionNode}),
     * using a cache to avoid repeated parsing.
     *
     * @param expression the expression to compile
     * @return the compiled {@link ExpressionNode}
     */
    public ExpressionNode compile(String expression) {
        Cache.Key      key    = Cache.Key.forObject(expression);
        ExpressionNode cached = cache.get(key);

        if (cached == null) {
            TokenizableSource source =
                    new StringSource("EXPRESSION(" + expression + ")", expression);
            TokenCursor cursor = lexer.tokenize(source);
            cursor.currentIf(BasicToken.T_SOL);
            cached = (ExpressionNode) parser.parse(cursor, context);
            cache.put(key, cached);
        }

        return cached;
    }

    /**
     * Evaluates the specified expression in the given context, converting the result to the desired type.
     *
     * @param expression the expression to evaluate
     * @param context    the {@link EvaluationContext} to use
     * @param type       the target result type
     * @return the evaluated and converted result
     */
    public <T> T evaluate(String expression, EvaluationContext context, Map<String, Object> data, Class<T> type) {
        data.forEach(context::setValue);
        return context.getConversion().convert(compile(expression).evaluate(context), type);
    }

    /**
     * Evaluates the specified expression in the given context, converting the result to the desired type.
     *
     * @param expression the expression to evaluate
     * @param context    the {@link EvaluationContext} to use
     * @param type       the target result type
     * @return the evaluated and converted result
     */
    public <T> T evaluate(String expression, EvaluationContext context, Class<T> type) {
        return evaluate(expression, context, Collections.emptyMap(), type);
    }

    /**
     * Evaluates the specified expression in the given context, returning an Object.
     *
     * @param expression the expression to evaluate
     * @param context    the {@link EvaluationContext} to use
     * @return the evaluated result
     */
    public Object evaluate(String expression, EvaluationContext context) {
        return evaluate(expression, context, Object.class);
    }

    /**
     * Evaluates the specified expression using a new context, converting the result to the desired type.
     *
     * @param expression the expression to evaluate
     * @param type       the target result type
     * @return the evaluated and converted result
     */
    public <T> T evaluate(String expression, Class<T> type) {
        return evaluate(expression, newContext(), type);
    }

    /**
     * Evaluates the specified expression using a new context, returning an Object.
     *
     * @param expression the expression to evaluate
     * @return the evaluated result
     */
    public Object evaluate(String expression) {
        return evaluate(expression, Object.class);
    }
}
