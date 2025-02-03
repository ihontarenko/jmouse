package org.jmouse.core.convert;

import org.jmouse.core.reflection.JavaType;

import java.util.Objects;

/**
 * A simple utility class that holds a pair of {@link Class} references, typically
 * used to represent some kind of mapping or conversion from {@code classA} to
 * {@code classB}. The {@code ClassPair} is parameterized to ensure type safety
 * in scenarios like converters or adapters.
 *
 * @param <A> the type of the first class
 * @param <B> the type of the second class
 */
public class ClassPair<A, B> {

    private final Class<A> classA;
    private final Class<B> classB;

    /**
     * Constructs a new {@code ClassPair} with the specified classes.
     *
     * @param classA the first class in the pair
     * @param classB the second class in the pair
     */
    public ClassPair(Class<A> classA, Class<B> classB) {
        this.classA = classA;
        this.classB = classB;
    }

    /**
     * Returns the second class in the pair.
     *
     * @return a {@link Class} object representing type {@code B}
     */
    public Class<B> getClassB() {
        return classB;
    }

    /**
     * Returns the first class in the pair.
     *
     * @return a {@link Class} object representing type {@code A}
     */
    public Class<A> getClassA() {
        return classA;
    }

    /**
     * Checks if the two classes in the pair are the same.
     * <p>
     * This method compares {@link Class} objects {@code classA} and {@code classB} to determine if they represent
     * the same type.
     * </p>
     *
     * @return {@code true} if the two classes in the pair are the same, {@code false} otherwise
     */
    public boolean isTheSame() {
        return Objects.equals(classA, classB);
    }

    /**
     * Determines equality by comparing the underlying {@link Class} objects
     * for both {@code classA} and {@code classB}.
     *
     * @param object the other object to compare
     * @return {@code true} if both pairs hold the same classes, otherwise {@code false}
     */
    @Override
    public boolean equals(Object object) {
        if (object != null && getClass() == object.getClass()) {
            ClassPair<?, ?> classPair = (ClassPair<?, ?>) object;
            return Objects.equals(classA, classPair.classA)
                    && Objects.equals(classB, classPair.classB);
        }
        return false;
    }

    /**
     * Computes the hash code based on the underlying class references.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(classA, classB);
    }

    /**
     * Returns a string representation of the object.
     */
    @Override
    public String toString() {
        return "'%s' → '%s'".formatted(JavaType.forClass(classA), JavaType.forClass(classB));
    }
}
