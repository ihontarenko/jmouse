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

        // register generic converters
        conversion.registerConverter(new StringToEnumConverter());

        // register basic converters
        conversion.registerConverter(new NumberToStringConverter());
        conversion.registerConverter(new NumberToNumberConverter());

        System.out.println("end");
//        ApplicationBeanContext context = WebApplicationLauncher.launch(StartApplication.class);
    }

}
