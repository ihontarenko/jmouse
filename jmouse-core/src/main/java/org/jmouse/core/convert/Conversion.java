package org.jmouse.core.convert;

/**
 * An extension of the {@link ConverterFactory} interface that provides convenient
 * conversion methods in addition to managing a registry of converters. It allows
 * both registration of converters and direct execution of conversions, making
 * it a one-stop interface for all conversion-related operations.
 *
 * @see ConverterFactory
 */
public interface Conversion extends ConverterFactory {

    /**
     * Converts the specified {@code source} bean from one type ({@code sourceType})
     * to another ({@code targetType}), using a registered converter if available.
     *
     * @param <T>        the type of the source bean
     * @param <R>        the type of the desired result
     * @param source     the bean to be converted (may be {@code null})
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @return the converted bean, or {@code null} if no matching converter was found
     *         or if {@code source} was {@code null}
     */
    <T, R> R convert(T source, Class<T> sourceType, Class<R> targetType);

    /**
     * A convenience method that infers the source type from the runtime class of the
     * provided {@code source} bean. If {@code source} is {@code null}, this method
     * immediately returns {@code null}.
     *
     * @param <T>        the type of the source bean
     * @param <R>        the type of the desired result
     * @param source     the bean to be converted (may be {@code null})
     * @param targetType the class representing the target type
     * @return the converted bean, or {@code null} if no matching converter was found
     *         or if {@code source} was {@code null}
     */
    @SuppressWarnings("unchecked")
    default <T, R> R convert(T source, Class<R> targetType) {
        R converted = null;
        if (source != null) {
            Class<T> sourceType = (Class<T>) source.getClass();
            converted = convert(source, sourceType, targetType);
        }
        return converted;
    }
}
