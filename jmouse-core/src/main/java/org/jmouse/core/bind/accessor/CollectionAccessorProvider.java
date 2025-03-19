package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.util.Priority;

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
