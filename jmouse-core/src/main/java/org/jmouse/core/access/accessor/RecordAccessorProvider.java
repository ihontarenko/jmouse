package org.jmouse.core.access.accessor;

import org.jmouse.core.Priority;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

@Priority(-2100)
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
