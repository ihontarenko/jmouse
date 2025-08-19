package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.net.CIDR;

import java.util.Set;

public class CIDRToStringConverter implements GenericConverter<CIDR, String> {

    @Override
    public String convert(CIDR source, Class<CIDR> sourceType, Class<String> targetType) {
        return source.toString();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(CIDR.class, String.class)
        );
    }

}
