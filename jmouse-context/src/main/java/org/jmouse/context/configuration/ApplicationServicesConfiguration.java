package org.jmouse.context.configuration;

import org.jmouse.core.convert.converter.*;
import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.convert.DefaultConversion;
import org.jmouse.core.convert.converter.enums.IntegerToEnumConverter;
import org.jmouse.core.convert.converter.enums.StringToEnumConverter;
import org.jmouse.core.mapping.Mapping;
import org.jmouse.core.mapping.MappingFactory;
import org.jmouse.context.ApplicationBeanContext;

@Configuration
public class ApplicationServicesConfiguration {

    @Provide
    public Mapping mappingService(ApplicationBeanContext context) {
        return MappingFactory.create(context.getBaseClasses());
    }

    @Provide(proxied = true)
    public Conversion conversionService() {
        Conversion conversion = new DefaultConversion();

        conversion.registerConverter(new StringToEnumConverter());
        conversion.registerConverter(new IntegerToEnumConverter());
        conversion.registerConverter(new NumberToStringConverter());
        conversion.registerConverter(new StringToNumberConverter());
        conversion.registerConverter(new NumberToNumberConverter());

        DateAndTimeConverters.getConverters().forEach(conversion::registerConverter);
        CollectionConverters.getConverters().forEach(conversion::registerConverter);

        return conversion;
    }

}
