package org.jmouse.el.extension.attribute;

import java.util.Map;

public class MapAttributeResolver implements AttributeResolver {

    @Override
    public Object resolve(Object instance, String name) {
        Object value = null;

        if (instance instanceof Map<?, ?> map) {
            value = map.get(name);
        }

        return value;
    }

}
