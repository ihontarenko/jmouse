package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class BooleanToStringConverter implements GenericConverter<Boolean, String> {

    @Override
    public String convert(Boolean source, Class<Boolean> sourceType, Class<String> targetType) {
        return source.toString();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(Boolean.class, String.class),
                ClassPair.of(boolean.class, Boolean.class)
        );
    }

}
