package org.jmouse.el.extension;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.i18n.StandardMessageSourceBundle;
import org.jmouse.el.evaluation.EvaluationContext;

import java.util.ArrayList;
import java.util.List;

public class i18nExtension implements Extension {

    @Override
    public List<Function> getFunctions() {
        return List.of(new i18nFunction());
    }

    public static class i18nFunction implements Function {

        private final StandardMessageSourceBundle messageSource;
        private final List<String>                names = new ArrayList<>();

        public i18nFunction() {
            messageSource = new StandardMessageSourceBundle(getClass().getClassLoader());
            messageSource.setFallbackWithCode(true);
            messageSource.setFallbackPattern("{? %s ?}");
        }

        @Override
        public Object execute(Arguments arguments, EvaluationContext context) {
            Conversion conversion = context.getConversion();
            String     name       = conversion.convert(arguments.getFirst(), String.class);
            String     key        = conversion.convert(arguments.get(1), String.class);
            Object[]   parameters = arguments.stream().skip(2).toArray();

            if (!names.contains(name)) {
                names.add(name);
                messageSource.addNames(name);
            }

            return messageSource.getMessage(key, parameters);
        }

        @Override
        public String getName() {
            return "i18n";
        }
    }

}
