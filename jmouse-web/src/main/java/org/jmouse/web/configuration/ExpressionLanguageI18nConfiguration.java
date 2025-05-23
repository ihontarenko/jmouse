package org.jmouse.web.configuration;

import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.i18nExtension;

import java.util.Set;

@Configuration
public class ExpressionLanguageI18nConfiguration {

    @Provide("el-extensions")
    public Set<Extension> expressionLanguageExtensions() {
        return Set.of(new i18nExtension());
    }

}
