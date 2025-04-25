package org.jmouse.el.parser;

import org.jmouse.el.AbstractObjectContainer;

public class ParserContainer extends AbstractObjectContainer<Class<? extends Parser>, Parser> {

    @Override
    public Class<? extends Parser> keyFor(Parser extension) {
        return extension.getClass();
    }

    @Override
    public String toString() {
        return "PARSERS: " + super.toString();
    }

}
