package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.convert.GenericConverter;

import java.lang.reflect.Array;
import java.util.Set;

public class ObjectArraysConverter implements GenericConverter<Object[], Object[]> {

    private final Conversion conversion;

    public ObjectArraysConverter(Conversion conversion) {
        this.conversion = conversion;
    }

    @Override
    public Object[] convert(Object[] source, Class<Object[]> sourceType, Class<Object[]> targetType) {
        int      arraySize           = source.length;
        Object   targetArray         = Array.newInstance(Object.class, arraySize);

        for (int i = 0; i < arraySize; i++) {
            Object                                       sourceValue         = source[i];
            Class<?>                                     sourceComponentType = sourceValue != null
                    ? sourceValue.getClass() : sourceType.getComponentType();
            Class<?>                                     targetComponentType = sourceComponentType.isArray()
                    ? sourceComponentType : targetType.getComponentType();
            GenericConverter<Object, Object>             genericConverter    = conversion.findConverter(
                    ClassPair.of(sourceComponentType, targetComponentType));
            @SuppressWarnings("unchecked") Class<Object> typeA               = (Class<Object>) sourceComponentType;
            @SuppressWarnings("unchecked") Class<Object> typeB               = (Class<Object>) targetComponentType;
            Array.set(targetArray, i, genericConverter.convert(sourceValue, typeA, typeB));
        }

        return (Object[]) targetArray;
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(ClassPair.of(Object[].class, Object[].class));
    }

}
