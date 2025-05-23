package org.jmouse.context.configuration;

import org.jmouse.beans.annotation.BeanCollection;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.extension.Extension;

import java.util.Set;

@Configuration
public class ExpressionLanguageConfiguration {

    @Provide
    public ExpressionLanguage expressionLanguage(@BeanCollection Set<Extension> extensions) {
        ExpressionLanguage el = new ExpressionLanguage();

        if (extensions != null) {
            extensions.forEach(el.getExtensions()::importExtension);
        }

        return el;
    }

    @Provide
    public Extension getDefaultExtension() {
        return new Extension() { };
    }

}
