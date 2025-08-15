package org.jmouse.core.convert.converter;

import org.jmouse.core.Bytes;
import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class BytesToStringConverter implements GenericConverter<Bytes, String> {

    @Override
    public String convert(Bytes source, Class<Bytes> sourceType, Class<String> targetType) {
        return source.toString();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(Bytes.class, String.class)
        );
    }

}
