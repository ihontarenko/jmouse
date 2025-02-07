package org.jmouse.core.bind;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.Converter;
import org.jmouse.core.convert.StandardConversion;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.convert.converter.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class BinderConversion extends StandardConversion {

    public BinderConversion() {
        registerConverter(new StringToNumberConverter());
        registerConverter(new NumberToStringConverter());

        for (GenericConverter<?, ?> converter : CollectionConverters.getConverters()) {
            registerConverter(converter);
        }

        for (GenericConverter<?, ?> converter : DateAndTimeConverters.getConverters()) {
            registerConverter(converter);
        }

        ClassPair<?, ?> classPair = new ClassPair<>(String.class, List.class);
        removeConverter(classPair);
        registerConverter(String.class, List.class, new StringToCollectionConverter());
    }

    @SuppressWarnings("rawtypes")
    private static class StringToCollectionConverter implements Converter<String, List> {

        @Override
        public List<Object> convert(String source) {
            List<String> list = List.of(source);

            if (source.indexOf(',') != -1) {
                list = Stream.of(source.split(",")).map(String::trim).toList();
            }

            return Collections.singletonList(list);
        }

    }

}
