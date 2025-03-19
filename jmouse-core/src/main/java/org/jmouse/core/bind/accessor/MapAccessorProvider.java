package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

public class MapAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isMap();
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new MapAccessor(source);
    }

}
