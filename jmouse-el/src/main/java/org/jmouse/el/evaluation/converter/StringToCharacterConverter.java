package org.jmouse.el.evaluation.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class StringToCharacterConverter implements GenericConverter<String, Character> {

    @Override
    public Character convert(String source, Class<String> sourceType, Class<Character> targetType) {
        return source.charAt(0);
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(ClassPair.of(String.class, Character.class));
    }

}
