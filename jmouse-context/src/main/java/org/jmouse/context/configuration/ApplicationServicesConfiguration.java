package org.jmouse.context.configuration;

import org.jmouse.core.convert.PredefinedConversion;
import org.jmouse.core.convert.converter.*;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.core.convert.Conversion;
import org.jmouse.common.mapping.Mapping;
import org.jmouse.common.mapping.MappingFactory;
import org.jmouse.context.ApplicationBeanContext;

@Configuration
public class ApplicationServicesConfiguration {

    @Provide
    public Mapping mappingService(ApplicationBeanContext context) {
        return MappingFactory.create(context.getBaseClasses());
    }

    @Provide(proxied = true)
    public Conversion conversionService() {
        Conversion conversion = new PredefinedConversion() {
        };

        DateAndTimeConverters.getConverters().forEach(conversion::registerConverter);
        CollectionConverters.getConverters().forEach(conversion::registerConverter);

        return conversion;
    }

}
