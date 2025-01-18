package svit.web;

import svit.converter.Converter;
import svit.converter.ConverterFactory;
import svit.converter.Conversion;

public class StartApplication {

    public static void main(String... arguments) {
        ConverterFactory converterFactory = new Conversion();
        converterFactory.registerConverter(new StringToIntegerConverter());
        converterFactory.registerConverter(new StringToDoubleConverter());
        System.out.println("end");
//        ApplicationBeanContext context = WebApplicationLauncher.launch(StartApplication.class);
    }

    static class StringToIntegerConverter implements Converter<String, Integer> {

        @Override
        public Integer convert(String source) {
            return Integer.parseInt(source);
        }
    }

    static class StringToDoubleConverter implements Converter<String, Double> {

        @Override
        public Double convert(String source) {
            return Double.parseDouble(source);
        }
    }



}
