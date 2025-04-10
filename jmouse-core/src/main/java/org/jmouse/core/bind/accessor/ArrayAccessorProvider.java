package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.util.Priority;

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
