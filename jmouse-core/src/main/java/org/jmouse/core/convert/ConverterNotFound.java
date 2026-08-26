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
    public ConverterNotFound(ClassPair classPair) {
        super("Unable to convert %s — no registered converter found.".formatted(classPair));
    }

    /**
     * Constructs a new {@code ConverterNotFound} exception for a converter asked for by <em>name</em>.
     *
     * <p>⚠️ The available names are part of the message, not an afterthought. A name reaches this
     * lookup from a text file somebody typed, and the overwhelmingly likely fault is a typo or a
     * missing namespace — both of which the list answers on sight, and neither of which "no such
     * converter" answers at all.</p>
     *
     * @param name      the name that was asked for
     * @param available every name a converter is registered under
     */
    public ConverterNotFound(String name, java.util.Collection<String> available) {
        super(available.isEmpty()
                      ? "No converter is named '%s', and none is named at all.".formatted(name)
                      : "No converter is named '%s'. Registered names: %s."
                              .formatted(name, String.join(", ", available)));
    }

}
