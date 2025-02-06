package org.jmouse.util;

/**
 * Utility class Maths for performing basic mathematical operations.
 *
 * <p>This class supports operations on various numeric types by utilizing generics.
 * It operates on objects of type {@link Number}, allowing compatibility with
 * subclasses such as {@code Integer}, {@code Double}, {@code Float}, and others.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * public static void main(String[] args) {
 *     System.out.println(Maths.sum(5, 3.2)); // 8.2
 *     System.out.println(Maths.divide(10, 2)); // 5.0
 *     System.out.println(Maths.multiply(4, 2.5)); // 10.0
 *     System.out.println(Maths.subtract(10, 7)); // 3.0
 *     System.out.println(Maths.power(2, 3)); // 8.0
 *     System.out.println(Maths.abs(-5)); // 5.0
 * }
 * }</pre>
 */
final public class Maths {

    /**
     * Adds two numbers and returns the result as {@code double}.
     *
     * @param a the first number, of type {@link Number}
     * @param b the second number, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return the sum of {@code a} and {@code b} as {@code double}
     * @throws NullPointerException if either {@code a} or {@code b} is {@code null}
     *
     * <h2>Example:</h2>
     * <pre>{@code
     * double result = MathFunctions.sum(5, 3.2); // 8.2
     * }</pre>
     */
    public static <T extends Number, U extends Number> double sum(T a, U b) {
        return a.doubleValue() + b.doubleValue();
    }

    /**
     * Divides one number by another and returns the result as {@code double}.
     *
     * @param a the dividend, of type {@link Number}
     * @param b the divisor, of type {@link Number}
     * @param <T> the type of the dividend
     * @param <U> the type of the divisor
     * @return the result of {@code a / b} as {@code double}
     * @throws ArithmeticException if {@code b} is zero
     * @throws NullPointerException if either {@code a} or {@code b} is {@code null}
     *
     * <h2>Example:</h2>
     * <pre>{@code
     * double result = MathFunctions.divide(10, 2); // 5.0
     * }</pre>
     */
    public static <T extends Number, U extends Number> double divide(T a, U b) {
        if (b.doubleValue() == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return a.doubleValue() / b.doubleValue();
    }

    /**
     * Multiplies two numbers and returns the result as {@code double}.
     *
     * @param a the first multiplier, of type {@link Number}
     * @param b the second multiplier, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return the product of {@code a * b} as {@code double}
     *
     * <h2>Example:</h2>
     * <pre>{@code
     * double result = MathFunctions.multiply(4, 2.5); // 10.0
     * }</pre>
     */
    public static <T extends Number, U extends Number> double multiply(T a, U b) {
        return a.doubleValue() * b.doubleValue();
    }

    /**
     * Subtracts the second number from the first and returns the result as {@code double}.
     *
     * @param a the minuend, of type {@link Number}
     * @param b the subtrahend, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return the result of {@code a - b} as {@code double}
     *
     * <h2>Example:</h2>
     * <pre>{@code
     * double result = MathFunctions.subtract(10, 7); // 3.0
     * }</pre>
     */
    public static <T extends Number, U extends Number> double subtract(T a, U b) {
        return a.doubleValue() - b.doubleValue();
    }

    /**
     * Calculates the power of a base raised to an exponent and returns the result as {@code double}.
     *
     * @param base the base number, of type {@link Number}
     * @param exponent the exponent, of type {@link Number}
     * @param <T> the type of the base number
     * @param <U> the type of the exponent
     * @return the result of {@code base^exponent} as {@code double}
     *
     * <h2>Example:</h2>
     * <pre>{@code
     * double result = MathFunctions.power(2, 3); // 8.0
     * }</pre>
     */
    public static <T extends Number, U extends Number> double power(T base, U exponent) {
        return Math.pow(base.doubleValue(), exponent.doubleValue());
    }

    /**
     * Computes the absolute value of a number and returns it as {@code double}.
     *
     * @param a the number, of type {@link Number}
     * @param <T> the type of the number
     * @return the absolute value of {@code a} as {@code double}
     *
     * <h2>Example:</h2>
     * <pre>{@code
     * double result = MathFunctions.abs(-5); // 5.0
     * }</pre>
     */
    public static <T extends Number> double abs(T a) {
        return Math.abs(a.doubleValue());
    }
}

