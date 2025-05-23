package org.jmouse.context.configuration;

import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.ExtensionContainer;

import java.util.Set;

@Configuration
public class ExpressionLanguageConfiguration {

    @Provide
    public ExpressionLanguage expressionLanguage(@Qualifier("el-extensions") Set<Extension> extensions) {
        ExpressionLanguage el = new ExpressionLanguage();

        if (!extensions.isEmpty()) {
            ExtensionContainer container = el.getExtensions();
            extensions.forEach(container::importExtension);
        }

        return el;
    }

}
