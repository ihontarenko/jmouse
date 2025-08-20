package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.geo.Country;

import java.util.Set;

public class CountryToStringConverter implements GenericConverter<Country, String> {

    @Override
    public String convert(Country source, Class<Country> sourceType, Class<String> targetType) {
        return source.toString();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(Country.class, String.class)
        );
    }

}
