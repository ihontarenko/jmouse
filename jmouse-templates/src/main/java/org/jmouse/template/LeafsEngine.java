package org.jmouse.template;

import org.jmouse.el.StringSource;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.*;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.rendering.RenderableNode;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.lexer.TemplateRecognizer;
import org.jmouse.template.lexer.TemplateTokenizer;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.parsing.TemplateParser;

import java.io.Reader;
import java.io.StringReader;

public class LeafsEngine implements Engine {

    private ExtensionContainer         extensions;
    private Cache<Cache.Key, Template> cache;
    private TemplateLoader<String>     loader;
    private Lexer                      lexer;
    private ParserContext              parserContext;

    public LeafsEngine() {
        initialize();
    }

    public void initialize() {
        this.cache = Cache.memory();
        this.lexer = new DefaultLexer(new TemplateTokenizer(new TemplateRecognizer()));
        this.parserContext = new DefaultParserContext(this.extensions);
        this.extensions = new StandardExtensionContainer();
        this.extensions.importExtension(new TemplateCoreExtension());
    }

    @Override
    public Template getTemplate(String name) {
        Template  cached;
        Cache.Key cacheKey = Cache.Key.forObject(name);

        if (cache.contains(cacheKey)) {
            cached = cache.get(cacheKey);
        } else {
            Reader   reader   = loadTemplate(name);
            Template template = parseTemplate(name, reader);
            // add new compiled template to cache
            cache.put(cacheKey, template);
            cached = template;
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

        // skip T_SOL
        cursor.currentIf(BasicToken.T_SOL);
        RenderableNode root = (RenderableNode) parserContext.getParser(TemplateParser.class).parse(cursor, parserContext);

        return new StandardTemplate(root, source, this);
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
