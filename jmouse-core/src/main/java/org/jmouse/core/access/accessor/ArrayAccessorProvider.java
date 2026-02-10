package org.jmouse.core.access.accessor;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.core.Priority;

@Priority(-900)
public class ArrayAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isArray();
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new ArrayAccessor(source);
    }

}
