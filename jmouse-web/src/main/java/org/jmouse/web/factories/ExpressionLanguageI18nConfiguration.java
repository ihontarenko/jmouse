package org.jmouse.web.factories;

import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.i18nExtension;

@Factories
public class ExpressionLanguageI18nConfiguration {

    @Provide
    public Extension i18nExtension() {
        return new i18nExtension();
    }

}
