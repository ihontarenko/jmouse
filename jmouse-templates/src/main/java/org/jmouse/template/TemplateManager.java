package org.jmouse.template;

import org.jmouse.template.loader.TemplateLoader;

public interface TemplateManager {

    Template getTemplate(String name);

    Template getTemplate(String name, TemplateLoader<String> loader);

}
