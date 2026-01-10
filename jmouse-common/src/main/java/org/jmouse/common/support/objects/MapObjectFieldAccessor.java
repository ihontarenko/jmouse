package org.jmouse.common.support.objects;

import java.util.Map;

public class MapObjectFieldAccessor implements FieldAccessor {

    private final Map<String, Object> objectMap;
    private final String              fieldName;

    public MapObjectFieldAccessor(Map<String, Object> objectMap, String fieldName) {
        this.objectMap = objectMap;
        this.fieldName = fieldName;
    }

    @Override
    public Object getValue() {
        return objectMap.get(fieldName);
    }

    @Override
    public void setValue(Object value) {
        objectMap.put(fieldName, value);
    }

}
