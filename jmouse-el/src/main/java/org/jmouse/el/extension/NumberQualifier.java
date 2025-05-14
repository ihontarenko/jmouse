package org.jmouse.el.extension;

public enum NumberQualifier {

    BYTE('B'),
    SHORT('S'),
    CHARACTER('C'),
    INT('I'),
    LONG('L'),
    FLOAT('F'),
    DOUBLE('D');

    private final char identifier;

    NumberQualifier(char identifier) {
        this.identifier = identifier;
    }

    public char getIdentifier() {
        return identifier;
    }

    public static NumberQualifier find(String value) {
        return find(value.charAt(0));
    }

    public static NumberQualifier find(char character) {
        NumberQualifier qualifier = null;

        for (NumberQualifier candidate : values()) {
            if (candidate.identifier == character) {
                qualifier = candidate;
            }
        }

        return qualifier;
    }

}
