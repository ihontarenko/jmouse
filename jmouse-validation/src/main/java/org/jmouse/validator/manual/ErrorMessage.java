package org.jmouse.validator.manual;

public record ErrorMessage(String pointer, String message, String code) {

    @Override
    public String toString() {
        return "[%s] %s".formatted(pointer, message);
    }

}