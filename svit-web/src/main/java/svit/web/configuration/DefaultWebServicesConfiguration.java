package svit.web.configuration;

import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;
import svit.convert.Conversion;
import svit.convert.DefaultConversion;
import svit.convert.converter.IntegerToEnumConverter;
import svit.convert.converter.NumberToNumberConverter;
import svit.convert.converter.NumberToStringConverter;
import svit.convert.converter.StringToEnumConverter;
import svit.mapping.Mapping;
import svit.mapping.MappingFactory;
import svit.web.context.ApplicationBeanContext;

@Configuration
public class DefaultWebServicesConfiguration {

    @Provide
    public Mapping createMapping(ApplicationBeanContext context) {
        return MappingFactory.create(context.getBaseClasses());
    }

    @Provide(proxied = true)
    public Conversion createConversion() {
        Conversion conversion = new DefaultConversion();

//        conversion.registerConverter(new NumberToStringConverter());
        conversion.registerConverter(new StringToEnumConverter());
        conversion.registerConverter(new IntegerToEnumConverter());
        conversion.registerConverter(new NumberToNumberConverter());

        return conversion;
    }

}
