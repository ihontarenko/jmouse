package org.jmouse.core.convert.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class MediaTypeToStringConverter implements GenericConverter<MediaType, String> {

    @Override
    public String convert(MediaType source, Class<MediaType> sourceType, Class<String> targetType) {
        return source.toString();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(MediaType.class, String.class)
        );
    }

}
