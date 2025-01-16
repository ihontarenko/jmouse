package svit.util;

/**
 * Utility class Booleans for performing common boolean comparisons.
 *
 * <p>This class provides static methods for comparing two numeric values of any type that extends {@link Number}.
 * It supports operations such as greater than, greater than or equal to, less than, less than or equal to,
 * equality, and inequality checks.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * public static void main(String[] args) {
 *     System.out.println(Booleans.gt(5, 3));   // true
 *     System.out.println(Booleans.lte(2, 2)); // true
 *     System.out.println(Booleans.eq(4.5, 4)); // false
 * }
 * }</pre>
 */
final public class Booleans {

    /**
     * Checks if the first number is greater than the second.
     *
     * @param a the first number, of type {@link Number}
     * @param b the second number, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return {@code true} if {@code a > b}, {@code false} otherwise
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T extends Number, U extends Number> boolean gt(T a, U b) {
        return a.doubleValue() > b.doubleValue();
    }

    /**
     * Checks if the first number is greater than or equal to the second.
     *
     * @param a the first number, of type {@link Number}
     * @param b the second number, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return {@code true} if {@code a >= b}, {@code false} otherwise
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T extends Number, U extends Number> boolean gte(T a, U b) {
        return a.doubleValue() >= b.doubleValue();
    }

    /**
     * Checks if the first number is less than the second.
     *
     * @param a the first number, of type {@link Number}
     * @param b the second number, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return {@code true} if {@code a < b}, {@code false} otherwise
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T extends Number, U extends Number> boolean lt(T a, U b) {
        return a.doubleValue() < b.doubleValue();
    }

    /**
     * Checks if the first number is less than or equal to the second.
     *
     * @param a the first number, of type {@link Number}
     * @param b the second number, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return {@code true} if {@code a <= b}, {@code false} otherwise
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T extends Number, U extends Number> boolean lte(T a, U b) {
        return a.doubleValue() <= b.doubleValue();
    }

    /**
     * Checks if the first number is equal to the second.
     *
     * @param a the first number, of type {@link Number}
     * @param b the second number, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return {@code true} if {@code a == b}, {@code false} otherwise
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T extends Number, U extends Number> boolean eq(T a, U b) {
        return Double.compare(a.doubleValue(), b.doubleValue()) == 0;
    }

    /**
     * Checks if the first number is not equal to the second.
     *
     * @param a the first number, of type {@link Number}
     * @param b the second number, of type {@link Number}
     * @param <T> the type of the first number
     * @param <U> the type of the second number
     * @return {@code true} if {@code a != b}, {@code false} otherwise
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T extends Number, U extends Number> boolean neq(T a, U b) {
        return Double.compare(a.doubleValue(), b.doubleValue()) != 0;
    }
}
