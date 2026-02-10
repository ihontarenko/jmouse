package org.jmouse.core.access.accessor;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.core.Priority;

@Priority(-1000)
public class CollectionAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isCollection();
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new CollectionAccessor(source);
    }

}
