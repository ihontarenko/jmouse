package org.jmouse.el.parsing;

import org.jmouse.el.AbstractObjectContainer;

public class ParserContainer extends AbstractObjectContainer<Class<? extends Parser>, Parser> {

    @Override
    public Class<? extends Parser> keyFor(Parser extension) {
        return extension.getClass();
    }

}
