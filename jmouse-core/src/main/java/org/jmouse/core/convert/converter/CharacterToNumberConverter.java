package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.Converter;
import org.jmouse.core.convert.ConverterNotFound;
import org.jmouse.core.convert.GenericConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CharacterToNumberConverter implements GenericConverter<Character, Number> {

    private final Map<Class<? extends Number>, Converter<Character, Number>> converters = new HashMap<>();

    public CharacterToNumberConverter() {
        converters.put(Byte.class, source -> (byte) source.charValue());
        converters.put(Short.class, source -> (short) source.charValue());
        converters.put(Long.class, source -> (long) source);
        converters.put(Integer.class, source -> (int) source);
        converters.put(Double.class, source -> (double) source);
        converters.put(Float.class, source -> (float) source);
    }

    @Override
    public Number convert(Character source, Class<Character> sourceType, Class<Number> targetType) {
        Converter<Character, Number> converter = converters.get(targetType);

        if (converter == null) {
            throw new ConverterNotFound(new ClassPair(sourceType, targetType));
        }

        return converter.convert(source);
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(Character.class, Byte.class),
                ClassPair.of(Character.class, Short.class),
                ClassPair.of(Character.class, Long.class),
                ClassPair.of(Character.class, Integer.class),
                ClassPair.of(Character.class, Double.class),
                ClassPair.of(Character.class, Float.class)
        );
    }

}
