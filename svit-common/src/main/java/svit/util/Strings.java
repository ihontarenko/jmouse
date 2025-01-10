package svit.util;

import static java.lang.Character.*;

final public class Strings {

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String uncapitalize(final String value) {
        String result = value;

        if (value != null && value.length() > 0) {
            char[] chars = value.toCharArray();
            chars[0] = toLowerCase(chars[0]);
            result = new String(chars);
        }

        return result;
    }

    public static String underscored(final String value){
        return underscored(value, false);
    }

    public static String underscored(final String value, final boolean toUpperCase) {
        String result = value;

        if (value != null && !value.isBlank()) {
            StringBuilder builder  = new StringBuilder();
            char          previous = ' ';
            int           counter  = 0;

            for (char current : value.toCharArray()) {
                char newCharacter = toUpperCase ? toUpperCase(current) : toLowerCase(current);
                boolean lowToHigh = isUpperCase(current) && isLowerCase(previous) && !isWhitespace(previous);
                // boolean highToLow = isLowerCase(current) && isUpperCase(previous) && !isWhitespace(previous);

                if (lowToHigh && isLetter(previous) && counter > 1) {
                    builder.append('_').append(newCharacter);
                } else {
                    builder.append(newCharacter);
                }

                counter++;
                previous = current;
            }

            result = builder.toString();
        }

        return result;
    }

    public static String appendBefore(int number, char character, int length) {
        return number < (int) Math.pow(10, length) - 1
                ? new String(new char[(int) (length - Math.floor(Math.log10(number)) - 1)]).replace('\0', character) + number
                : String.valueOf(number);
    }

}
