package org.jmouse.core.convert;

import org.jmouse.core.convert.converter.*;
import org.jmouse.core.convert.converter.enums.EnumToScalarConverter;
import org.jmouse.core.convert.converter.enums.IntegerToEnumConverter;
import org.jmouse.core.convert.converter.enums.StringToEnumConverter;
import org.jmouse.core.reflection.Reflections;

abstract public class PredefinedConversion extends StandardConversion {

    public PredefinedConversion() {
        // default number-to-string and vise versa converters
        registerConverter(new StringToNumberConverter());
        registerConverter(new NumberToStringConverter());
        // default enum to string, integer converters
        registerConverter(new StringToEnumConverter());
        registerConverter(new IntegerToEnumConverter());
        registerConverter(new EnumToScalarConverter());
        // boolean to string and vice versa
        registerConverter(new BooleanToStringConverter());
        registerConverter(new StringToBooleanConverter());

        // collection and array converters
        for (GenericConverter<?, ?> converter : CollectionConverters.getConverters()) {
            registerConverter(converter);
        }

        // converters for data-time values
        for (GenericConverter<?, ?> converter : DateAndTimeConverters.getConverters()) {
            registerConverter(converter);
        }

        // javaClass-to-string and vice versa
        registerConverter(String.class, Class.class, new PredefinedConversion.StringToClassConverter());
        // bytes-to-string and vice versa
        registerConverter(new StringToBytesConverter());
        registerConverter(new BytesToStringConverter());
        // mediaType-to-string and vice versa
        registerConverter(new StringToMediaTypeConverter());
        registerConverter(new MediaTypeToStringConverter());
        // cidr-to-string and vice versa
        registerConverter(new StringToCIDRConverter());
        registerConverter(new CIDRToStringConverter());
        // enum country and vice versa
        registerConverter(new StringToCountryConverter());
        registerConverter(new CountryToStringConverter());
        // string-pattern and vice versa
        registerConverter(new StringToPatternConverter());
        registerConverter(new PatternToStringConverter());
    }

    @SuppressWarnings({"rawtypes"})
    private static class StringToClassConverter implements Converter<String, Class> {
        @Override
        public Class<?> convert(String source) {
            return Reflections.getClassFor(source);
        }
    }

}
