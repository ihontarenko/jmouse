package org.jmouse.java.bytecode.vocabulary;

public record JvmTokenRange(int minimum, int maximum) {
    public boolean contains(int value) {
        return minimum <= value && value <= maximum;
    }
}
