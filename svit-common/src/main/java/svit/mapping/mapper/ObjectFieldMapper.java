package svit.mapping.mapper;

import svit.mapping.Mapper;
import svit.reflection.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ObjectFieldMapper implements Mapper<Object, Map<String, Object>> {

    @Override
    public Map<String, Object> map(Object source) {
        Map<String, Object> data = new HashMap<>();

        map(source, data);

        return data;
    }

    @Override
    public Object reverse(Map<String, Object> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void map(Object source, Map<String, Object> destination) {
        for (Field field : Reflections.getClassFields(source.getClass(), Modifier.PRIVATE)) {
            destination.put(field.getName(), Reflections.getFieldValue(source, field));
        }
    }

    @Override
    public void reverse(Map<String, Object> source, Object destination) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Reflections.setFieldValue(destination, entry.getKey(), entry.getValue());
        }
    }

}
