package org.jmouse.template;

import org.jmouse.template.extension.ExtensionContainer;
import org.jmouse.template.loader.TemplateLoader;

public interface TemplateManager {

    String resolveTemplate(String name);

    Template loadTemplate(String name);

    Template getTemplate(String name);

    Template getTemplate(String name, TemplateLoader<String> loader);

    ExtensionContainer getExtensions();



}
