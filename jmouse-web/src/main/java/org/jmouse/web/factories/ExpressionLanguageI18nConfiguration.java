package org.jmouse.web.factories;

import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.i18nExtension;

@BeanFactories
public class ExpressionLanguageI18nConfiguration {

    @Bean
    public Extension i18nExtension() {
        return new i18nExtension();
    }

}
