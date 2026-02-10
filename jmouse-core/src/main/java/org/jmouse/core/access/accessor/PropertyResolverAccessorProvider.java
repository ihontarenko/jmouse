package org.jmouse.core.access.accessor;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorProvider;
import org.jmouse.core.environment.PropertyResolver;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.core.Priority;

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
