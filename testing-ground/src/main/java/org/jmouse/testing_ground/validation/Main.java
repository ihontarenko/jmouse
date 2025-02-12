package org.jmouse.testing_ground.validation;

import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.bean.PropertyAccessor;
import org.jmouse.util.Getter;
import org.jmouse.validator.Validator;

import java.util.Map;

public class Main {

    public static void main(String... arguments) {

        Validator validator = Validator.forInstance(User.class, (user, errors) -> {

            if (user == null) {
                errors.reject("user.null", "User must be non NULL");
            }

            JavaBeanDescriptor<User> descriptor = JavaBeanDescriptor.forBean(User.class, user);
            PropertyAccessor<User>   accessor   = descriptor.getProperty("name").getPropertyAccessor();

            System.out.println(accessor.obtainValue(user)); // John



            assert user != null;
            if (user.getName() == null || user.getName().isEmpty()) {
                errors.rejectValue("user.name", "to be localized and be replaced ", "failed", null);
                errors.rejectValue("user.email", "API_ERROR_NOT_LOCALIZED", "failed", null);
            }
        });

        Validator mapValidator = Validator.forInstance(Map.class, (map, errors) -> {
            Getter<Map<String, Object>, Object> getter = Getter.ofMap("name");

            if (map == null) {

            } else {

            }

            System.out.println(getter.get(map));
        });


        validator.validate(new User("John", "passwd123", "john@example.com"));
        mapValidator.validate(Map.of("name", "John", "email", "john@example.com"));

        System.out.println("Validation passed.");

    }

}
