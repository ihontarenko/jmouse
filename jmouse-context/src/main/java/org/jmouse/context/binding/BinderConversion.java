package org.jmouse.context.binding;

import org.jmouse.core.convert.DefaultConversion;
import org.jmouse.core.convert.converter.NumberToNumberConverter;
import org.jmouse.core.convert.converter.StringToNumberConverter;

public class BinderConversion extends DefaultConversion {

    public BinderConversion() {
        registerConverter(new NumberToNumberConverter());
        registerConverter(new StringToNumberConverter());
        registerConverter(new NumberToNumberConverter());
    }

}
