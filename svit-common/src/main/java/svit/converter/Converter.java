package svit.converter;

/**
 * A functional interface representing a simple conversion operation from a source
 * type {@code S} to a target type {@code T}. This interface is commonly used in
 * contexts where objects of one type need to be transformed into another type,
 * such as data mapping or serialization.
 *
 * <p>Usage example:
 * <pre>{@code
 * // Convert a String to an Integer
 * Converter<String, Integer> stringToInteger = Integer::valueOf;
 * Integer result = stringToInteger.convert("123");
 * // result will be 123
 *
 * // Another example: Convert an Integer to a String
 * Converter<Integer, String> integerToString = Object::toString;
 * String text = integerToString.convert(123);
 * // text will be "123"
 * }</pre>
 *
 * @param <S> the source type
 * @param <T> the target type
 */
@FunctionalInterface
public interface Converter<S, T> {

    /**
     * Converts the specified source object of type {@code S} to an object of type {@code T}.
     *
     * @param source the source object to convert
     * @return the converted object
     */
    T convert(S source);
}
