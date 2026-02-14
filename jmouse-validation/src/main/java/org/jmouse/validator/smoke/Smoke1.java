package org.jmouse.validator.smoke;

import jakarta.validation.Validation;
import org.jmouse.validator.*;

public class Smoke1 {

    public static void main(String... arguments) {
        var jakartaValidator = Validation.buildDefaultValidatorFactory().getValidator();

//        User user = new User(new Profile("mail@localhost"));
        User user = new User(new Profile(""), "John".intern());

        DefaultValidatorRegistry registry = new DefaultValidatorRegistry();

        registry.register(Validator.forInstance(User.class, (u, e) -> {
            if (u.name() == null || u.name().isBlank()) {
                e.rejectValue("name", "notBlank", "Name is required");
            }
        }));

        Validator.forInstance(User.class, (u, e) -> {
            if (u.name() == null || !u.name().isBlank()) {
                e.rejectValue("name", "notBlank", "Name is required");
            }
        }).validate(user).getErrors().getFirst().getDefaultMessage();

        ValidationProcessor processor = ValidationProcessors.builder()
                .validatorRegistry(registry)     // registry == provider тут автоматично ок
                .errorsFactory(new DefaultErrorsFactory())
                .validationPolicy(ValidationPolicy.COLLECT_ALL)
                .build();

        ValidationResult<User> result = processor.validate(user, "user", ValidationHints.of("create"));

    }

    public record User(Profile profile, String name) {
    }

    public record Profile(String email) {
    }


}
