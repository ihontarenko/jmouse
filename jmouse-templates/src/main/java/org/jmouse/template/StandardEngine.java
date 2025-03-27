package org.jmouse.template;

import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.lexer.TemplateRecognizer;
import org.jmouse.template.lexer.TemplateTokenizer;
import org.jmouse.template.loader.TemplateLoader;

public class StandardEngine implements Engine {

    private final ExtensionContainer     extensions;
    private final Configuration          configuration;
    private       TemplateLoader<String> loader;
    private       Lexer                  lexer;

    public StandardEngine(Configuration configuration) {
        this.configuration = configuration;
        this.extensions = new StandardExtensionContainer();
        initialize();
    }

    public void initialize() {
        this.lexer = new DefaultLexer(new TemplateTokenizer(new TemplateRecognizer()));
        this.getExtensions().importExtension(new TemplateCoreExtension());
    }

    @Override
    public Template createTemplate(String name) {
        return null;
    }

    @Override
    public Template getTemplate(String name) {
        return null;
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }
}
