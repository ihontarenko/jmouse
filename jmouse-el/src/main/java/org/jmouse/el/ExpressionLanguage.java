package org.jmouse.el;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.NullLiteralNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.template.Cache;

import java.util.Collections;
import java.util.Map;

/**
 * 🔎 Engine for parsing, compiling, caching, and evaluating expressions.
 * <p>
 * Manages a lexer, parser, and cache for {@link Expression} instances, as well as
 * an {@link ExtensionContainer} for custom functions, operators, and filters.
 * </p>
 */
public class ExpressionLanguage {

    private final static ExpressionLanguage SINGLETON = new ExpressionLanguage();

    private final ParserContext                context;
    private final Lexer                        lexer;
    private final ExpressionParser             parser;
    private final Cache<Cache.Key, Expression> cache;
    private final ExtensionContainer           extensions;

    /**
     * Creates an expression language with fully customized components.
     */
    public ExpressionLanguage(
            ExtensionContainer extensions,
            Lexer lexer,
            ParserContext context,
            Cache<Cache.Key, Expression> cache,
            ExpressionParser parser
    ) {
        this.extensions = extensions;
        this.lexer = lexer;
        this.context = context;
        this.cache = cache;
        this.parser = parser;
    }

    /**
     * Creates an expression language using the specified parser type * resolved from the parser context.
     */
    public ExpressionLanguage(
            ExtensionContainer extensions,
            Lexer lexer, ParserContext context,
            Cache<Cache.Key, Expression> cache,
            Class<? extends ExpressionParser> parserType
    ) {
        this(extensions, lexer, context, cache, (ExpressionParser) context.getParser(parserType));
    }

    /**
     * Creates an expression language using the default ExpressionParser * resolved from the parser context.
     */
    public ExpressionLanguage(
            ExtensionContainer extensions, Lexer lexer, ParserContext context, Cache<Cache.Key, Expression> cache
    ) {
        this(extensions, lexer, context, cache, ExpressionParser.class);
    }

    /**
     * Creates an expression language with custom extensions, lexer, * parser context and parser type.
     */
    public ExpressionLanguage(ExtensionContainer extensions, Lexer lexer, ParserContext context, Class<? extends ExpressionParser> parserType) {
        this(extensions, lexer, context, Cache.memory(), parserType);
    }

    /**
     * Creates an expression language with custom extensions, lexer * and parser context.
     */
    public ExpressionLanguage(ExtensionContainer extensions, Lexer lexer, ParserContext context) {
        this(extensions, lexer, context, Cache.memory(), ExpressionParser.class);
    }

    /**
     * Creates an expression language with custom extensions, lexer * and parser type.
     */
    public ExpressionLanguage(ExtensionContainer extensions, Lexer lexer, Class<? extends ExpressionParser> parserType) {
        this(extensions, lexer, new DefaultParserContext(extensions), Cache.memory(), parserType);
    }

    /**
     * Creates an expression language with custom extensions and lexer.
     */
    public ExpressionLanguage(ExtensionContainer extensions, Lexer lexer) {
        this(extensions, lexer, new DefaultParserContext(extensions), Cache.memory(), ExpressionParser.class);
    }

    /**
     * Creates an expression language with custom extensions * and parser type.
     */
    public ExpressionLanguage(ExtensionContainer extensions, Class<? extends ExpressionParser> parserType) {
        this(extensions, defaultLexer(), new DefaultParserContext(extensions), Cache.memory(), parserType);
    }

    /**
     * Creates an expression language with custom extensions.
     */
    public ExpressionLanguage(ExtensionContainer extensions) {
        this(extensions, defaultLexer(), new DefaultParserContext(extensions), Cache.memory(), ExpressionParser.class);
    }

    /**
     * Creates an expression language with custom lexer and parser type.
     */
    public ExpressionLanguage(Lexer lexer, Class<? extends ExpressionParser> parserType) {
        this(defaultExtensions(), lexer, new DefaultParserContext(defaultExtensions()), Cache.memory(), parserType);
    }

    /**
     * Creates an expression language with custom lexer.
     */
    public ExpressionLanguage(Lexer lexer) {
        this(defaultExtensions(), lexer, new DefaultParserContext(defaultExtensions()), Cache.memory(), ExpressionParser.class);
    }

    /**
     * Creates an expression language with custom parser context * and parser type.
     */
    public ExpressionLanguage(ParserContext context, Class<? extends ExpressionParser> parserType) {
        this(context.getExtensionContainer(), defaultLexer(), context, Cache.memory(), parserType);
    }

    /**
     * Creates an expression language with custom parser context.
     */
    public ExpressionLanguage(ParserContext context) {
        this(context, ExpressionParser.class);
    }

    /**
     * Creates an expression language with custom cache * and parser type.
     */
    public ExpressionLanguage(Cache<Cache.Key, Expression> cache, Class<? extends ExpressionParser> parserType) {
        this(defaultExtensions(), defaultLexer(), new DefaultParserContext(defaultExtensions()), cache, parserType);
    }

    /**
     * Creates an expression language with custom cache.
     */
    public ExpressionLanguage(Cache<Cache.Key, Expression> cache) {
        this(defaultExtensions(), defaultLexer(), new DefaultParserContext(defaultExtensions()), cache, ExpressionParser.class);
    }

    /**
     * Creates an expression language with custom extensions, * cache and parser type.
     */
    public ExpressionLanguage(ExtensionContainer extensions, Cache<Cache.Key, Expression> cache, Class<? extends ExpressionParser> parserType) {
        this(extensions, defaultLexer(), new DefaultParserContext(extensions), cache, parserType);
    }

    /**
     * Creates an expression language with custom extensions and cache.
     */
    public ExpressionLanguage(ExtensionContainer extensions, Cache<Cache.Key, Expression> cache) {
        this(extensions, defaultLexer(), new DefaultParserContext(extensions), cache, ExpressionParser.class);
    }

    /**
     * Creates an expression language with default configuration * and custom parser type.
     */
    public ExpressionLanguage(Class<? extends ExpressionParser> parserType) {
        this(defaultExtensions(), defaultLexer(), new DefaultParserContext(defaultExtensions()), Cache.memory(), parserType);
    }

    /**
     * Creates an expression language with default configuration.
     */
    public ExpressionLanguage() {
        this(defaultExtensions(), defaultLexer(), new DefaultParserContext(defaultExtensions()), Cache.memory(), ExpressionParser.class);
    }

    private static ExtensionContainer defaultExtensions() {
        StandardExtensionContainer container = new StandardExtensionContainer();
        container.importExtension(new CoreExtension());
        return container;
    }

    private static Lexer defaultLexer() {
        return new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()));
    }

    public static ExpressionLanguage getSingleton() {
        return SINGLETON;
    }

    /**
     * Creates a fresh {@link EvaluationContext} for expression evaluation,
     * initialized with this engine's extensions.
     *
     * @return a new {@link EvaluationContext}
     */
    public EvaluationContext newContext() {
        EvaluationContext context = new DefaultEvaluationContext();
        context.setExtensions(extensions);
        return context;
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
     * Compiles the given expression string into an AST ({@link Expression}),
     * using a cache to avoid repeated parsing.
     *
     * @param expression the expression to compile
     * @return the compiled {@link Expression}
     */
    public Expression compile(String expression) {
        Cache.Key  key    = Cache.Key.forObject(expression);
        Expression cached = cache.get(key);

        if (expression == null || expression.isBlank()) {
            return new NullLiteralNode();
        }

        if (cached == null) {
            TokenizableSource source = new StringSource("EXPRESSION(" + expression + ")", expression);
            TokenCursor cursor = lexer.tokenize(source);
            cursor.consumeIf(BasicToken.T_SOL);
            cached = (Expression) parser.parse(cursor, context);
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
