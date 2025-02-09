package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private String special;

    @Override
    public void initialize(StrongPassword annotation) {
        special = annotation.special();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext constraintValidatorContext) {
        if (password == null || password.isBlank()) {
            return true;
        }

        boolean         hasUpperCase   = password.chars().anyMatch(Character::isUpperCase);
        boolean         hasLowerCase   = password.chars().anyMatch(Character::isLowerCase);
        boolean         hasDigit       = password.chars().anyMatch(Character::isDigit);
        boolean         hasSpecialChar = password.chars().anyMatch(i -> special.indexOf(i) >= 0);

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }

}
