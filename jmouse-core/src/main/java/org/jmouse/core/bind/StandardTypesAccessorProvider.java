package org.jmouse.core.bind;

import org.jmouse.core.reflection.TypeInformation;

public class StandardTypesAccessorProvider implements ObjectAccessorProvider{

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
