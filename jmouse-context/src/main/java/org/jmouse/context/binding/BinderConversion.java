package org.jmouse.context.binding;

import org.jmouse.core.convert.DefaultConversion;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.convert.converter.*;

public class BinderConversion extends DefaultConversion {

    public BinderConversion() {
        registerConverter(new StringToNumberConverter());
        registerConverter(new NumberToStringConverter());

        for (GenericConverter<?, ?> converter : CollectionConverters.getConverters()) {
            registerConverter(converter);
        }

        for (GenericConverter<?, ?> converter : DateAndTimeConverters.getConverters()) {
            registerConverter(converter);
        }
    }

}
