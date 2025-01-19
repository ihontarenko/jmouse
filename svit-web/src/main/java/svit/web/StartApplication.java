package svit.web;

import svit.convert.DefaultConversion;
import svit.convert.GenericConverter;
import svit.convert.converter.NumberToNumberConverter;
import svit.convert.converter.NumberToStringConverter;
import svit.convert.converter.StringToEnumConverter;

import java.math.BigDecimal;

public class StartApplication {

    enum Color{RED, GREEN, BLUE}

    public static void main(String... arguments) {
        DefaultConversion conversion = new DefaultConversion();

        GenericConverter<String, Enum<?>> converter = new StringToEnumConverter();

        // register generic converters
        conversion.registerConverter(new StringToEnumConverter());

        // register basic converters
        conversion.registerConverter(String.class, Integer.class, Integer::parseInt);
        conversion.registerConverter(String.class, Double.class, Double::parseDouble);
        conversion.registerConverter(String.class, Float.class, Float::parseFloat);
        conversion.registerConverter(String.class, BigDecimal.class, BigDecimal::new);
        conversion.registerConverter(new NumberToStringConverter());
        conversion.registerConverter(new NumberToNumberConverter());

        System.out.println("end");
//        ApplicationBeanContext context = WebApplicationLauncher.launch(StartApplication.class);
    }

}
