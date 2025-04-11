package org.jmouse.el.evaluation.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

public class StringToBooleanConverter implements GenericConverter<String, Boolean> {

    @Override
    public Boolean convert(String source, Class<String> sourceType, Class<Boolean> targetType) {
        return Boolean.parseBoolean(source);
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(ClassPair.of(String.class, Boolean.class));
    }

}
