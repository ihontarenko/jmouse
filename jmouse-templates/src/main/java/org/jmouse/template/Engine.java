package org.jmouse.template;

import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.rendering.Template;

public interface Engine {

    Template getTemplate(String name);

    ExtensionContainer getExtensions();

}
