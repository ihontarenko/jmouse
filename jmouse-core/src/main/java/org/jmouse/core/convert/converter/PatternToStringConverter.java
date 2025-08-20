package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;
import java.util.regex.Pattern;

public class PatternToStringConverter implements GenericConverter<Pattern, String> {

    @Override
    public String convert(Pattern source, Class<Pattern> sourceType, Class<String> targetType) {
        return source.pattern();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(Pattern.class, String.class)
        );
    }

}
