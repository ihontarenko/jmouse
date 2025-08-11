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

        // custom converters
        registerConverter(String.class, Class.class, new PredefinedConversion.StringToClassConverter());
        registerConverter(new StringToBytesConverter());
    }

    @SuppressWarnings({"rawtypes"})
    private static class StringToClassConverter implements Converter<String, Class> {
        @Override
        public Class<?> convert(String source) {
            return Reflections.getClassFor(source);
        }
    }

}
