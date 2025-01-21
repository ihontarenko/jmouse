package svit.web;

import svit.convert.Conversion;
import svit.convert.DefaultConversion;
import svit.convert.converter.NumberToNumberConverter;
import svit.convert.converter.NumberToStringConverter;
import svit.convert.converter.StringToEnumConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

public class StartApplication {

    enum Color{RED, GREEN, BLUE}

    public static void main(String... arguments) {
        Conversion conversion = new DefaultConversion();

        // register generic converters
        conversion.registerConverter(new StringToEnumConverter());

        conversion.registerConverter(Integer.class, Long.class, Integer::longValue);
        conversion.registerConverter(Long.class, BigInteger.class, BigInteger::valueOf);
        conversion.registerConverter(Long.class, Double.class, Long::doubleValue);
        conversion.registerConverter(Long.class, BigDecimal.class, BigDecimal::valueOf);
        conversion.registerConverter(BigInteger.class, Instant.class, value
                 -> Instant.ofEpochMilli(value.longValue()));
        conversion.registerConverter(Instant.class, String.class, Instant::toString);
        conversion.registerConverter(Instant.class, LocalDateTime.class, instant -> LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId()));

        // register basic converters
//        conversion.registerConverter(new NumberToStringConverter());
//        conversion.registerConverter(new NumberToNumberConverter());

        System.out.println(conversion.convert(765765, Instant.class));
        System.out.println("end");
//        ApplicationBeanContext context = WebApplicationLauncher.launch(StartApplication.class);
    }

}
