package svit.util;

import static java.lang.Character.*;

/**
 * A utility class for working with and manipulating {@link String} objects.
 * This class provides common operations such as checking for emptiness,
 * uncapitalizing, converting to underscored format, and padding with characters.
 */
public final class Strings {

    private Strings() {
        // Prevent instantiation
    }

    /**
     * Checks if a string is not empty (i.e., not null and not blank).
     *
     * @param value the string to check.
     * @return {@code true} if the string is not empty, {@code false} otherwise.
     */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    /**
     * Checks if a string is empty (i.e., null or blank).
     *
     * @param value the string to check.
     * @return {@code true} if the string is empty, {@code false} otherwise.
     */
    public static boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Converts the first character of the given string to lowercase.
     *
     * @param value the string to uncapitalize.
     * @return the uncapitalized string, or the original string if null or empty.
     */
    public static String uncapitalize(final String value) {
        if (isEmpty(value)) {
            return value;
        }

        char[] chars = value.toCharArray();
        chars[0] = toLowerCase(chars[0]);
        return new String(chars);
    }

    /**
     * Converts a camelCase string to an underscored (snake_case) format.
     * The resulting string will be in lowercase.
     *
     * @param value the string to convert.
     * @return the underscored string, or the original string if null or empty.
     */
    public static String underscored(final String value) {
        return underscored(value, false);
    }

    /**
     * Converts a camelCase string to an underscored (snake_case) format.
     * The resulting string will be either in lowercase or uppercase, depending on the provided flag.
     *
     * @param value       the string to convert.
     * @param toUpperCase whether to convert the result to uppercase.
     * @return the underscored string, or the original string if null or empty.
     */
    public static String underscored(final String value, final boolean toUpperCase) {

        if (isEmpty(value)) {
            return value;
        }

        StringBuilder builder  = new StringBuilder();
        char          previous = ' ';
        int           counter  = 0;

        for (char current : value.toCharArray()) {
            char    newCharacter = toUpperCase ? toUpperCase(current) : toLowerCase(current);
            boolean lowToHigh    = isUpperCase(current) && isLowerCase(previous) && !isWhitespace(previous);

            if (lowToHigh && isLetter(previous) && counter > 0) {
                builder.append('_');
            }

            builder.append(newCharacter);

            counter++;
            previous = current;
        }

        return builder.toString();
    }

    /**
     * Pads a number with a specified character to ensure it reaches a specific length.
     * The padding is added before the number.
     *
     * @param number    the number to pad.
     * @param character the padding character.
     * @param length    the total length of the resulting string (including the number).
     * @return the padded string representation of the number.
     */
    public static String appendBefore(int number, char character, int length) {
        int numberLength  = (int) Math.floor(Math.log10(number)) + 1;
        int paddingLength = Math.max(0, length - numberLength);

        return paddingLength > 0
                ? String.valueOf(character).repeat(paddingLength) + number : String.valueOf(number);
    }
}
