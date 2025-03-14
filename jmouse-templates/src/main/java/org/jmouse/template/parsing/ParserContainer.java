package org.jmouse.template.parsing;

import org.jmouse.template.AbstractObjectContainer;

public class ParserContainer extends AbstractObjectContainer<Class<? extends Parser>, Parser> {

    @Override
    public Class<? extends Parser> keyFor(Parser extension) {
        return extension.getClass();
    }

}
