package org.jmouse.template;

import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.loader.TemplateLoader;

public interface Engine {

    Template createTemplate(String name);

    Template getTemplate(String name);

    ExtensionContainer getExtensions();

}
