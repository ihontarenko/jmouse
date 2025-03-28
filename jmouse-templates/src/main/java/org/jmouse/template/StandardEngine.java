package org.jmouse.template;

import org.jmouse.el.StringSource;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.lexer.TemplateRecognizer;
import org.jmouse.template.lexer.TemplateTokenizer;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.parsing.TemplateParser;

import java.io.Reader;
import java.io.StringReader;

public class StandardEngine implements Engine {

    private final ExtensionContainer         extensions;
    private final Configuration              configuration;
    private final Cache<Cache.Key, Template> cache;
    private       TemplateLoader<String>     loader;
    private       Lexer                      lexer;
    private       ParserContext              parserContext;

    public StandardEngine(Configuration configuration) {
        this.configuration = configuration;
        this.cache = Cache.memory();
        this.extensions = new StandardExtensionContainer();
        initialize();
    }

    public void initialize() {
        this.lexer = new DefaultLexer(new TemplateTokenizer(new TemplateRecognizer()));
        this.parserContext = new DefaultParserContext(this.extensions);
        this.extensions.importExtension(new TemplateCoreExtension());
    }

    @Override
    public Template getTemplate(String name) {
        Template  cached   = null;
        Cache.Key cacheKey = Cache.Key.forObject(name);

        if (cache.contains(cacheKey)) {
            cached = cache.get(cacheKey);
        } else {

        }

        return cached;
    }

    public TokenCursor getTokenCursor(String sourceName, Reader reader) {
        return lexer.tokenize(new StringSource(sourceName, reader));
    }

    public TokenCursor getTokenCursor(String sourceName, String expression) {
        return lexer.tokenize(new StringSource(sourceName, new StringReader(expression)));
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }
}
