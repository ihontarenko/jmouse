package org.jmouse.core.bind.accessor;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.env.PropertyResolver;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.util.Priority;

@Priority(-2500)
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
