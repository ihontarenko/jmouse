package org.jmouse.validator.smoke;

import jakarta.validation.Validation;
import jakarta.validation.spi.ValidationProvider;
import org.hibernate.validator.HibernateValidator;

import java.util.ServiceLoader;

public class VCheck {
    public static void main(String[] args) {
        var validator = Validation
                .byProvider(HibernateValidator.class)
                .configure()
                .buildValidatorFactory()
                .getValidator();

        System.out.println("OK: " + validator);
    }
}