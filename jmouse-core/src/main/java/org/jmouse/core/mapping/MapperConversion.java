package org.jmouse.core.mapping;

import org.jmouse.core.convert.PredefinedConversion;
import org.jmouse.core.convert.converter.NumberToNumberConverter;

public class MapperConversion extends PredefinedConversion {

    public MapperConversion() {
        super();
        registerConverter(new NumberToNumberConverter());
    }

}
