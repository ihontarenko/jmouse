package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.net.CIDR;

import java.util.Set;

public class StringToCIDRConverter implements GenericConverter<String, CIDR> {

    @Override
    public CIDR convert(String source, Class<String> sourceType, Class<CIDR> targetType) {
        return CIDR.parse(source);
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(String.class, CIDR.class)
        );
    }

}
