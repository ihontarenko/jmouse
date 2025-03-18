package org.jmouse.core.bind;

import org.jmouse.core.env.PropertyResolver;
import org.jmouse.core.reflection.TypeInformation;

public class PropertyResolverAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).is(PropertyResolver.class);
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new PropertyResolverAccessor((PropertyResolver) source);
    }

}
