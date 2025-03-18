package org.jmouse.core.bind;

import org.jmouse.core.reflection.TypeInformation;

public class RecordObjectAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isRecord();
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new ValueObjectAccessor(source);
    }

}
