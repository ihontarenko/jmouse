package org.jmouse.core.bind;

import org.jmouse.core.convert.Converter;
import org.jmouse.core.convert.StandardConversion;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.convert.converter.*;
import org.jmouse.core.convert.converter.enums.IntegerToEnumConverter;
import org.jmouse.core.convert.converter.enums.StringToEnumConverter;
import org.jmouse.core.reflection.Reflections;

public class BinderConversion extends StandardConversion {

    public BinderConversion() {
        // default number-to-string and vise versa converters
        registerConverter(new StringToNumberConverter());
        registerConverter(new NumberToStringConverter());
        // default enum to string, integer converters
        registerConverter(new StringToEnumConverter());
        registerConverter(new IntegerToEnumConverter());

        // collection and array converters
        for (GenericConverter<?, ?> converter : CollectionConverters.getConverters()) {
            registerConverter(converter);
        }

        // converters for data-time values
        for (GenericConverter<?, ?> converter : DateAndTimeConverters.getConverters()) {
            registerConverter(converter);
        }

        // custom converters
        registerConverter(String.class, Class.class, new StringToClassConverter());
    }

    @SuppressWarnings({"rawtypes"})
    private static class StringToClassConverter implements Converter<String, Class> {
        @Override
        public Class<?> convert(String source) {
            return Reflections.getClassFor(source);
        }
    }

}
