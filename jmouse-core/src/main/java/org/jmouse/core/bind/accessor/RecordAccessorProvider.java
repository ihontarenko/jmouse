package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

public class RecordAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isRecord();
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new RecordAccessor(source);
    }

}
