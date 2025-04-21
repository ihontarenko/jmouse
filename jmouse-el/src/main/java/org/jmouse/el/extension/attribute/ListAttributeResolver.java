package org.jmouse.el.extension.attribute;

import java.util.List;

public class ListAttributeResolver implements AttributeResolver {

    @Override
    public Object resolve(Object instance, String name) {
        Object value = null;

        if (instance instanceof List<?> list) {
            value = list.get(Integer.parseInt(name));
        }

        return value;
    }

}
