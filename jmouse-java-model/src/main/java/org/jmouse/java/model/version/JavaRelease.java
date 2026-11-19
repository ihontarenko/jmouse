package org.jmouse.java.model.version;



/** A feature release number, not a host JDK update/build version. */
public record JavaRelease(int number) implements Comparable<JavaRelease> {
    public JavaRelease {
        if (number <= 0) {
            throw new IllegalArgumentException("A Java feature release must be positive");
        }
    }

    @Override
    public int compareTo(JavaRelease other) {
        return Integer.compare(number, other.number);
    }
}
