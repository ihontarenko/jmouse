package org.jmouse.template;

import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.loader.TemplateLoader;

import java.io.Reader;

public interface Engine {

    Template getTemplate(String name);

    ExtensionContainer getExtensions();

    TemplateLoader<String> getLoader();

    void setLoader(TemplateLoader<String> loader);

    Reader loadTemplate(String name);

    Template parseTemplate(String name, Reader reader);

    TokenCursor getTokenCursor(TokenizableSource source);

    TokenCursor getTokenCursor(String sourceName, Reader reader);

    TokenizableSource getSource(String sourceName, Reader reader);

    TokenCursor getTokenCursor(String sourceName, String expression);

}
