package svit.beans;

import java.util.Map;

public class MapBeanField implements BeanField {

    private final String              fieldName;
    private final Map<String, Object> objectMap;

    public MapBeanField(String fieldName, Map<String, Object> objectMap) {
        this.fieldName = fieldName;
        this.objectMap = objectMap;
    }

    @Override
    public String getName() {
        return fieldName;
    }

    @Override
    public FieldAccessor getFieldAccessor() {
        return new MapObjectFieldAccessor(objectMap, fieldName);
    }

}

