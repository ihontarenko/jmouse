package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;
import java.util.regex.Pattern;

public class StringToPatternConverter implements GenericConverter<String, Pattern> {

    @Override
    public Pattern convert(String source, Class<String> sourceType, Class<Pattern> targetType) {
        return Pattern.compile(source);
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(String.class, Pattern.class)
        );
    }

}
