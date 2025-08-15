package org.jmouse.core.convert.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class StringToMediaTypeConverter implements GenericConverter<String, MediaType> {

    @Override
    public MediaType convert(String source, Class<String> sourceType, Class<MediaType> targetType) {
        return MediaType.forString(source);
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(String.class, MediaType.class)
        );
    }

}
