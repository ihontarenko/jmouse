package org.jmouse.testing_ground.validation;

import org.jmouse.validator.Rejected;
import org.jmouse.validator.Validator;
import test.application.InternalUser;
import test.application.User;

public class Main {

    public static void main(String... arguments) {

        Validator validator = Validator.forInstance(User.class, (user, errors) -> {
            if (user.getName() == null || user.getName().isEmpty()) {
                errors.rejectValue("user.name", "to be localized and be replaced ", "failed", null);
                errors.rejectValue("user.email", "API_ERROR_NOT_LOCALIZED", "failed", null);
            }
        });

//        Rejection.of(object)
//                        .errorCode("field.non_null")
//                .defaultMessage()
//                .field("name").nonNull()
//                .;

        validator.validate(new InternalUser());

    }

}
