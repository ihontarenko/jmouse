package org.jmouse.validator.jsr380;

public record ValidationGroups(Class<?>... groups) {
    public static ValidationGroups defaults() {
        return new ValidationGroups();
    }
}
