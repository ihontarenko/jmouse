package org.jmouse.template;

import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.template.loader.TemplateLoader;

public class DefaulTemplateManager implements TemplateManager {

    private final Configuration configuration;

    public DefaulTemplateManager(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String resolveTemplate(String name) {
        return "";
    }

    @Override
    public Template loadTemplate(String name) {
        return null;
    }

    @Override
    public Template getTemplate(String name) {
        return null;
    }

    @Override
    public Template getTemplate(String name, TemplateLoader<String> loader) {
        return null;
    }

    @Override
    public ExtensionContainer getExtensions() {
        return null;
    }
}
