package org.jmouse.core.bind;

import org.jmouse.core.convert.PredefinedConversion;
import org.jmouse.core.convert.converter.NumberToNumberConverter;

public class BinderConversion extends PredefinedConversion {

    public BinderConversion() {
        super();

        registerConverter(new NumberToNumberConverter());
    }

}
