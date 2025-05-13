package org.jmouse.el.evaluation.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class CharacterToStringConverter implements GenericConverter<Character, String> {

    @Override
    public String convert(Character source, Class<Character> sourceType, Class<String> targetType) {
        return source.toString();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(ClassPair.of(Character.class, String.class));
    }

}
