package org.jmouse.util.helper;

import java.net.URL;
import java.nio.file.FileSystems;

import static java.lang.Character.*;

/**
 * A utility class for working with and manipulating {@link String} objects.
 * This class provides common operations such as checking for emptiness,
 * uncapitalizing, converting to underscored format, and padding with characters.
 */
public final class Strings {

    public static String DIRECTORY_SEPARATOR = FileSystems.getDefault().getSeparator();

    private Strings() {
        // Prevent instantiation
    }

    /**
     * Extracts the suffix of a string based on the specified separator.
     */
    public static String suffix(String string, String separator) {
        return cut(string, separator, true, false, 0);
    }

    /**
     * Extracts the suffix of a string based on the specified separator.
     */
    public static String suffix(String string, String separator, boolean last) {
        return cut(string, separator, last, false, 0);
    }

    /**
     * Extracts the suffix of a string based on the specified separator and applies a offset.
     */
    public static String suffix(String string, String separator, boolean last, int offset) {
        return cut(string, separator, last, false, offset);
    }

    /**
     * Extracts the prefix of a string based on the specified separator.
     */
    public static String prefix(String string, String separator) {
        return cut(string, separator, true, true, 0);
    }

    /**
     * Extracts the prefix of a string based on the specified separator.
     */
    public static String prefix(String string, String separator, boolean last) {
        return cut(string, separator, last, true, 0);
    }

    /**
     * Extracts the prefix of a string based on the specified separator and applies a offset offset.
     */
    public static String prefix(String string, String separator, boolean last, int offset) {
        return cut(string, separator, last, true, offset);
    }

    /**
     * Cuts the string based on the specified separator, direction, and offset.
     */
    public static String cut(String string, String separator, boolean last, boolean prefix, int offset) {
        if (string == null) {
            return null;
        }

        int index = separator == null ? -1 : (last ? string.lastIndexOf(separator) : string.indexOf(separator));

        if (index != -1) {
            string = prefix ? string.substring(0, index + offset) : string.substring(index + offset);
        }

        return string;
    }

    /**
     * Extracts a substring between two delimiters.
     */
    public static String substring(String string, String start, String end) {
        if (string == null) {
            return null;
        }

        string = suffix(string, start, false, 1);
        string = prefix(string, end, true, 0);

        return string;
    }

    /**
     * Extracts a substring from the specified starting delimiter to the end of the string.
     */
    public static String substring(String string, String start) {
        return substring(string, start, null);
    }

    /**
     * Extracts the filename from a given path.
     */
    public static String filename(String name) {
        return (name.lastIndexOf(DIRECTORY_SEPARATOR) != -1)
                ? name.substring(name.lastIndexOf(DIRECTORY_SEPARATOR) + 1)
                : name;
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

    /**
     * Derives the fully qualified class name from a resource URL relative to the base class.
     *
     * @param baseClass the base class used to calculate the relative path
     * @param url       the URL of the resource
     * @return the fully qualified class name
     */
    public static String extractClassName(Class<?> baseClass, URL url) {
        String basePath = Files.packageToPath(baseClass, Files.SLASH);
        String relative = Files.getRelativePath(url, basePath);

        return Files.removeExtension(relative).replace(Files.SLASH.charAt(0), '.');
    }
}
