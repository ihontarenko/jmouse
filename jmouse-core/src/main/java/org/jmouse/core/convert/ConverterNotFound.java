package org.jmouse.core.convert;

/**
 * An exception that is thrown when no suitable converter is found for a given
 * pair of source and target types. This indicates that the conversion process
 * cannot proceed because a corresponding {@link GenericConverter} or {@link Converter}
 * has not been registered for the required type pair.
 */
public class ConverterNotFound extends RuntimeException {

    /**
     * Constructs a new {@code ConverterNotFound} exception with a message indicating
     * that no registered converter was found for the specified type pair.
     *
     * @param classPair the {@link ClassPair} representing the source and target types
     *                  for which no converter is available
     */
    public ConverterNotFound(ClassPair<?, ?> classPair) {
        super("Unable to convert %s — no registered converter found.".formatted(classPair));
    }

}
