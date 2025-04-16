package org.jmouse.el.renderable;

import org.jmouse.el.StringSource;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.renderable.lexer.TemplateRecognizer;
import org.jmouse.el.renderable.lexer.TemplateTokenizer;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.el.renderable.parser.TemplateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;

/**
 * TemplateEngine is the core class responsible for loading, parsing, and caching templates.
 * <p>
 * It initializes the necessary components including the extension container, lexer, parser context,
 * and cache. It also provides methods to load a template from a resource, tokenize its source,
 * parse it into an abstract syntax tree (AST), and create a new template instance.
 * </p>
 */
public class TemplateEngine implements Engine {

    private final Logger LOGGER = LoggerFactory.getLogger(TemplateEngine.class);

    private ExtensionContainer         extensions;
    private Cache<Cache.Key, Template> cache;
    private TemplateLoader<String>     loader;
    private Lexer                      lexer;
    private ParserContext              parserContext;

    /**
     * Constructs a new TemplateEngine and initializes its components.
     */
    public TemplateEngine() {
        initialize();
    }

    /**
     * Initializes the template engine by setting up the extension container, cache,
     * lexer, and parser context.
     * <p>
     * The extension container is configured with standard extensions (e.g. {@link TemplateCoreExtension}),
     * and the lexer is set up with a {@link TemplateTokenizer} and a {@link TemplateRecognizer}.
     * </p>
     */
    public void initialize() {
        this.extensions = new StandardExtensionContainer();
        this.extensions.importExtension(new TemplateCoreExtension());
        this.cache = Cache.memory();
        this.lexer = new DefaultLexer(new TemplateTokenizer(new TemplateRecognizer()));
        this.parserContext = new DefaultParserContext(this.extensions);
    }

    /**
     * Retrieves a template by its name.
     * <p>
     * The method checks the cache for an existing template instance. If found, it returns the cached template;
     * otherwise, it loads the template using {@link #loadTemplate(String)}, parses it, caches the new instance,
     * and returns it.
     * </p>
     *
     * @param name the name of the template to retrieve
     * @return the {@link Template} instance corresponding to the provided name
     */
    @Override
    public Template getTemplate(String name) {
        Template  cached;
        Cache.Key cacheKey = Cache.Key.forObject(name);

        if (cache.contains(cacheKey)) {
            cached = cache.get(cacheKey);

            LOGGER.info("Retrieved '{}' from cache", name);
        } else {
            Reader   reader   = loadTemplate(name);
            Template template = parseTemplate(name, reader);

            cache.put(cacheKey, template);
            cached = template;

            LOGGER.info("Parsed and cached new template '{}'", name);
        }

        return cached;
    }

    /**
     * Loads the raw template source for the specified template name.
     *
     * @param name the name of the template to load
     * @return a {@link Reader} for reading the template source
     */
    @Override
    public Reader loadTemplate(String name) {
        return loader.load(name);
    }

    /**
     * Parses the template source into an AST and creates a new template instance.
     * <p>
     * This method converts the raw source into a {@link TokenizableSource}, tokenizes it to produce a {@link TokenCursor},
     * and then invokes the appropriate parser to build the AST. The resulting template is then constructed.
     * </p>
     *
     * @param name   the name of the template
     * @param reader the reader for the template source
     * @return the parsed {@link Template} instance
     */
    @Override
    public Template parseTemplate(String name, Reader reader) {
        TokenizableSource source = getSource(name, reader);
        TokenCursor       cursor = getTokenCursor(source);
        Parser            parser = parserContext.getParser(TemplateParser.class);

        // Optionally skip starting token.
        cursor.currentIf(BasicToken.T_SOL);
        Node root = parser.parse(cursor, parserContext);

        return newTemplate(root, source);
    }

    /**
     * Creates a new template instance based on the provided AST root and source.
     *
     * @param root   the root node of the template AST
     * @param source the tokenizable source of the template
     * @return a new {@link Template} instance
     */
    @Override
    public Template newTemplate(Node root, TokenizableSource source) {
        return new DefaultTemplate(root, source, this);
    }

    /**
     * Converts the provided source name and reader into a {@link TokenizableSource}.
     *
     * @param sourceName the identifier for the source
     * @param reader     the reader for the source content
     * @return a {@link TokenizableSource} wrapping the provided source
     */
    @Override
    public TokenizableSource getSource(String sourceName, Reader reader) {
        return new StringSource(sourceName, reader);
    }

    /**
     * Tokenizes the given tokenizable source into a {@link TokenCursor}.
     *
     * @param source the source to tokenize
     * @return a {@link TokenCursor} for navigating through the tokens
     */
    @Override
    public TokenCursor getTokenCursor(TokenizableSource source) {
        return lexer.tokenize(source);
    }

    /**
     * Tokenizes the source provided as a reader along with its name.
     *
     * @param sourceName the name of the source
     * @param reader     the reader for the source
     * @return a {@link TokenCursor} for the source
     */
    @Override
    public TokenCursor getTokenCursor(String sourceName, Reader reader) {
        return getTokenCursor(new StringSource(sourceName, reader));
    }

    /**
     * Tokenizes the source provided as a string expression along with its name.
     *
     * @param sourceName the name of the expression
     * @param expression the string representing the expression
     * @return a {@link TokenCursor} for the expression
     */
    @Override
    public TokenCursor getTokenCursor(String sourceName, String expression) {
        return getTokenCursor(sourceName, new StringReader(expression));
    }

    /**
     * Returns the container for custom extensions used during template parsing and rendering.
     *
     * @return the {@link ExtensionContainer} instance
     */
    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }

    /**
     * Returns the current template loader.
     *
     * @return the {@link TemplateLoader} for String-based templates
     */
    @Override
    public TemplateLoader<String> getLoader() {
        return loader;
    }

    /**
     * Sets the template loader to be used by this engine.
     *
     * @param loader the {@link TemplateLoader} to set
     */
    @Override
    public void setLoader(TemplateLoader<String> loader) {
        this.loader = loader;
    }
}
