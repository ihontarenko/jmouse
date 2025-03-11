package org.jmouse.template.parser;

import org.jmouse.template.AbstractExtensionContainer;

public class ParserContainer extends AbstractExtensionContainer<Class<? extends Parser>, Parser> {

    @Override
    public Class<? extends Parser> key(Parser extension) {
        return extension.getClass();
    }

}
