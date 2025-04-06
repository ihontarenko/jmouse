package org.jmouse.el.core.parser;

import org.jmouse.el.core.AbstractObjectContainer;

public class ParserContainer extends AbstractObjectContainer<Class<? extends Parser>, Parser> {

    @Override
    public Class<? extends Parser> keyFor(Parser extension) {
        return extension.getClass();
    }

}
