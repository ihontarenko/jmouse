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

public class TemplateEngine implements Engine {

    private Logger LOGGER = LoggerFactory.getLogger(TemplateEngine.class);

    private ExtensionContainer         extensions;
    private Cache<Cache.Key, Template> cache;
    private TemplateLoader<String>     loader;
    private Lexer                      lexer;
    private ParserContext              parserContext;

    public TemplateEngine() {
        initialize();
    }

    public void initialize() {
        this.extensions = new StandardExtensionContainer();
        this.extensions.importExtension(new TemplateCoreExtension());
        this.cache = Cache.memory();
        this.lexer = new DefaultLexer(new TemplateTokenizer(new TemplateRecognizer()));
        this.parserContext = new DefaultParserContext(this.extensions);
    }

    @Override
    public Template getTemplate(String name) {
        Template  cached;
        Cache.Key cacheKey = Cache.Key.forObject(name);

        if (cache.contains(cacheKey)) {
            cached = cache.get(cacheKey);
            LOGGER.info("Get '{}' cached", name);
        } else {
            Reader   reader   = loadTemplate(name);
            Template template = parseTemplate(name, reader);
            cache.put(cacheKey, template);
            cached = template;
            LOGGER.info("Get '{}' new", name);
        }

        return cached;
    }

    @Override
    public Reader loadTemplate(String name) {
        return loader.load(name);
    }

    @Override
    public Template parseTemplate(String name, Reader reader) {
        TokenizableSource source = getSource(name, reader);
        TokenCursor       cursor = getTokenCursor(source);
        Parser            parser = parserContext.getParser(TemplateParser.class);

        // skip T_SOL
        cursor.currentIf(BasicToken.T_SOL);
        Node root = parser.parse(cursor, parserContext);

        return newTemplate(root, source);
    }

    @Override
    public Template newTemplate(Node root, TokenizableSource source) {
        return new DefaultTemplate(root, source, this);
    }

    @Override
    public TokenizableSource getSource(String sourceName, Reader reader) {
        return new StringSource(sourceName, reader);
    }

    @Override
    public TokenCursor getTokenCursor(TokenizableSource source) {
        return lexer.tokenize(source);
    }

    @Override
    public TokenCursor getTokenCursor(String sourceName, Reader reader) {
        return getTokenCursor(new StringSource(sourceName, reader));
    }

    @Override
    public TokenCursor getTokenCursor(String sourceName, String expression) {
        return getTokenCursor(sourceName, new StringReader(expression));
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }

    @Override
    public TemplateLoader<String> getLoader() {
        return loader;
    }

    @Override
    public void setLoader(TemplateLoader<String> loader) {
        this.loader = loader;
    }
}
