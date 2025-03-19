package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

public class StandardTypesAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        TypeInformation information = TypeInformation.forInstance(source);
        return information.isScalar() || information.isCollection() || information.isMap() || information.isArray();
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new StandardTypesAccessor(source);
    }

}
