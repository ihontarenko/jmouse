package svit.convert.converter;

import svit.convert.ClassPair;
import svit.convert.GenericConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

public class StringToNumberConverter implements GenericConverter<String, Number> {

    @Override
    public Number convert(String source, Class<String> sourceType, Class<Number> targetType) {
        return String.valueOf(source);
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
