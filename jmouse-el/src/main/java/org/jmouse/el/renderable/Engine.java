package org.jmouse.el.renderable;

import org.jmouse.el.core.extension.ExtensionContainer;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.lexer.TokenizableSource;
import org.jmouse.el.renderable.loader.TemplateLoader;

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
