package org.jmouse.testing_ground.validation;

import org.jmouse.core.i18n.StandardMessageSourceBundle;
import org.jmouse.testing_ground.i18n.MessageExample;
import org.jmouse.core.Getter;
import org.jmouse.validator.Errors;
import org.jmouse.validator.FieldError;
import org.jmouse.validator.ObjectError;
import org.jmouse.validator.Validator;

import java.util.Locale;
import java.util.Map;

public class Main {

    public static void main(String... arguments) throws Throwable {

        Validator validator = Validator.forInstance(User.class, (user, errors) -> {
            errors.reject("object.non_null", "User must be non NULL");
            errors.rejectValue("name", "field.name.empty", "User name {0} must be non NULL", user.getName());
        });

        Validator mapValidator = Validator.forInstance(Map.class, (map, errors) -> {
            Getter<Map<String, Object>, Object> getter = Getter.ofMap("name");
            System.out.println(getter.get(map));
        });


        StandardMessageSourceBundle messageSource = new StandardMessageSourceBundle(MessageExample.class.getClassLoader());

        messageSource.addNames("i18n.messages", "errors");

        Errors errors = validator.validate(new User("John", "passwd123", "john@binder.com"));

        for (ObjectError error : errors.getGlobalErrors()) {
            System.out.println(error.getCode());
            System.out.println(messageSource.getMessage(error, Locale.of("uk_UA")));
        }

        messageSource.addNames("i18n.validation");

        for (ObjectError error : errors.getGlobalErrors()) {
            System.out.println(messageSource.getMessage(error, Locale.of("uk_UA")));
        }

        for (FieldError error : errors.getErrors()) {
            System.out.println(messageSource.getMessage(error, Locale.of("uk_UA")));
            System.out.println(messageSource.getMessage(error));
        }

    }

}
