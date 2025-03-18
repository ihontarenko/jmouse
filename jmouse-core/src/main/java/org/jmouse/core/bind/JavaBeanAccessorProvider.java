package org.jmouse.core.bind;

import org.jmouse.core.reflection.TypeInformation;

public class JavaBeanAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isBean();
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new JavaBeanAccessor(source);
    }

}
