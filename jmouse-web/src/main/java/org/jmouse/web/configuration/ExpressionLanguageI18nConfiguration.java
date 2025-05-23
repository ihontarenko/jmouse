package org.jmouse.web.configuration;

import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.i18nExtension;

@Configuration
public class ExpressionLanguageI18nConfiguration {

    @Provide
    public Extension i18nExtension() {
        return new i18nExtension();
    }

}
