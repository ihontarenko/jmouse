package svit.convert.converter;

import svit.convert.*;
import svit.reflection.JavaTypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link GenericConverter} implementation that handles conversions between various
 * {@link Number} types, including both primitive and wrapper types. This converter
 * facilitates transforming a given {@code Number} into another specific {@code Number}
 * subtype by using predefined conversion functions.
 *
 * <p>The converter maintains a mapping of target number types to corresponding
 * {@link Converter} functions that perform the conversion. If a conversion is
 * requested for a type that is not supported, a {@link ConverterNotFound} exception
 * will be thrown.
 *
 * <p>Supported conversions include conversions among:
 * <ul>
 *   <li>Integer/int</li>
 *   <li>Long/long</li>
 *   <li>Double/double</li>
 *   <li>Float/float</li>
 *   <li>BigDecimal</li>
 *   <li>BigInteger</li>
 *   <li>Short/short</li>
 *   <li>Byte/byte</li>
 * </ul>
 *
 * @see GenericConverter
 * @see ConverterNotFound
 */
public class NumberToNumberConverter implements GenericConverter<Number, Number> {

    private final Map<Class<? extends Number>, Converter<Number, Number>> converters = new HashMap<>();

    /**
     * Constructs a new {@code NumberCrossConverter} and initializes the mapping
     * of target types to their corresponding conversion functions. It registers
     * converters for both primitive types and their corresponding wrapper types.
     */
    public NumberToNumberConverter() {
        converters.put(Integer.class, Number::intValue);
        converters.put(int.class, Number::intValue);
        converters.put(Long.class, Number::longValue);
        converters.put(long.class, Number::longValue);
        converters.put(Double.class, Number::doubleValue);
        converters.put(double.class, Number::doubleValue);
        converters.put(Float.class, Number::floatValue);
        converters.put(float.class, Number::floatValue);
        converters.put(BigDecimal.class, number -> BigDecimal.valueOf(number.doubleValue()));
        converters.put(BigInteger.class, number -> BigInteger.valueOf(number.longValue()));
        converters.put(Short.class, Number::shortValue);
        converters.put(short.class, Number::shortValue);
        converters.put(Byte.class, Number::byteValue);
        converters.put(byte.class, Number::byteValue);
    }

    /**
     * Converts the given {@code source} {@link Number} into an instance of the
     * specified {@code targetType}. It uses a predefined conversion function if
     * available.
     *
     * @param source     the number to convert
     * @param sourceType the class of the source number (unused in current logic)
     * @param targetType the desired target number type
     * @return the converted number as an instance of {@code targetType}
     * @throws ConverterNotFound if there is no registered converter for the given target type
     */
    @Override
    public Number convert(Number source, Class<Number> sourceType, Class<Number> targetType) {
        Converter<Number, Number> converter = converters.get(targetType);

        if (converter == null) {
            throw new ConverterNotFound(new ClassPair<>(sourceType, targetType));
        }

        return converter.convert(source);
    }

    /**
     * Returns a set of supported type pairs for conversion. Each pair represents a mapping
     * from a numeric source type to a numeric target type. This method generates a matrix
     * of all possible conversions among the numeric types defined in {@link JavaTypes}.
     *
     * @return a set of {@link ClassPair} objects representing all supported conversions
     *         between numeric primitive types and their wrappers
     */
    @Override
    public Set<ClassPair<? extends Number, ? extends Number>> getSupportedTypes() {
        Set<Class<? extends Number>>                       types     = new HashSet<>();
        Set<ClassPair<? extends Number, ? extends Number>> supported = new HashSet<>();

        types.addAll(JavaTypes.NUMBER_PRIMITIVES);
        types.addAll(JavaTypes.NUMBER_WRAPPERS);

        for (Class<? extends Number> sourceType : types) {
            for (Class<? extends Number> targetType : types) {
                supported.add(new ClassPair<>(sourceType, targetType));
            }
        }

        return supported;
    }
}
