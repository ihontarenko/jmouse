package org.jmouse.core.convert.converter;

import org.jmouse.core.Bytes;
import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class StringToBytesConverter implements GenericConverter<String, Bytes> {

    @Override
    public Bytes convert(String source, Class<String> sourceType, Class<Bytes> targetType) {
        return Bytes.parse(source);
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(String.class, Bytes.class)
        );
    }

}
