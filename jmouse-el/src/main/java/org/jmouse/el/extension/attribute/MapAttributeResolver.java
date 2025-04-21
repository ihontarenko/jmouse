package org.jmouse.el.extension.attribute;

import org.jmouse.core.bind.AttributeResolver;
import org.jmouse.util.Priority;

import java.util.Map;

@Priority(-100)
public class MapAttributeResolver implements AttributeResolver {

    /**
     * Determines whether this resolver can handle the specified instance.
     *
     * @param instance the target object instance to check
     * @return {@code true} if this resolver supports resolving attributes on the instance;
     * {@code false} otherwise
     */
    @Override
    public boolean supports(Object instance) {
        return instance instanceof Map;
    }

    @Override
    public Object resolve(Object instance, String name) {
        Object value = null;

        if (instance instanceof Map<?, ?> map) {
            value = map.get(name);
        }

        return value;
    }

}
