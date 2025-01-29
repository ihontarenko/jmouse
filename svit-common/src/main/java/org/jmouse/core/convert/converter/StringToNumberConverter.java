package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.Converter;
import org.jmouse.core.convert.ConverterNotFound;
import org.jmouse.core.convert.GenericConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StringToNumberConverter implements GenericConverter<String, Number> {

    private final Map<Class<? extends Number>, Converter<String, Number>> converters = new HashMap<>();

    public StringToNumberConverter() {
        converters.put(Integer.class, Integer::valueOf);
        converters.put(Long.class, Long::valueOf);
        converters.put(Double.class, Double::valueOf);
        converters.put(Float.class, Float::valueOf);
        converters.put(BigInteger.class, BigInteger::new);
        converters.put(BigDecimal.class, BigDecimal::new);
        converters.put(Short.class, Short::valueOf);
        converters.put(Byte.class, Byte::valueOf);
    }

    @Override
    public Number convert(String source, Class<String> sourceType, Class<Number> targetType) {
        Converter<String, Number> converter = converters.get(targetType);

        if (converter == null) {
            throw new ConverterNotFound(new ClassPair<>(sourceType, targetType));
        }

        return converter.convert(source);
    }

    @Override
    public Set<ClassPair<? extends String, ? extends Number>> getSupportedTypes() {
        return Set.of(
                new ClassPair<>(String.class, Short.class),
                new ClassPair<>(String.class, Integer.class),
                new ClassPair<>(String.class, Long.class),
                new ClassPair<>(String.class, Double.class),
                new ClassPair<>(String.class, Float.class),
                new ClassPair<>(String.class, BigInteger.class),
                new ClassPair<>(String.class, BigDecimal.class)
        );
    }
}
