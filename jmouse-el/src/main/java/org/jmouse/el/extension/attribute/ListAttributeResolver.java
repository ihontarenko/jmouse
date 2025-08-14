package org.jmouse.el.extension.attribute;

import org.jmouse.core.bind.AttributeResolver;
import org.jmouse.core.Priority;

import java.util.List;

@Priority(-1000)
public class ListAttributeResolver implements AttributeResolver {

    /**
     * Determines whether this resolver can handle the specified instance.
     *
     * @param instance the target object instance to check
     * @return {@code true} if this resolver supports resolving attributes on the instance;
     * {@code false} otherwise
     */
    @Override
    public boolean supports(Object instance) {
        return instance instanceof List;
    }

    @Override
    public Object resolve(Object instance, String name) {
        Object value = null;

        if (instance instanceof List<?> list) {
            value = list.get(Integer.parseInt(name));
        }

        return value;
    }

}
