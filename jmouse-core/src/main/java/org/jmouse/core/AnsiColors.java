package org.jmouse.core;

import java.util.stream.Collectors;

/**
 * {@code AnsiColors} defines ANSI escape codes for coloring, styling, and formatting
 * console output in terminals that support ANSI sequences.
 * <p>
 * This enumeration provides:
 * <ul>
 *     <li>Regular colors</li>
 *     <li>Bold text styles</li>
 *     <li>Underlined text styles</li>
 *     <li>Background colors</li>
 *     <li>High-intensity (bright) colors</li>
 *     <li>Bold high-intensity colors</li>
 *     <li>High-intensity background colors</li>
 * </ul>
 * <p>
 * Each constant holds the corresponding ANSI escape code string.
 * You can combine these codes with text in the console to produce colorful
 * and styled terminal output.
 * <p>
 * Example:
 * <pre>{@code
 * System.out.println(AnsiColors.RED_BOLD + "Error!" + AnsiColors.RESET);
 * }</pre>
 */
public enum AnsiColors {

    RESET("\u001B[0m"),
    // REGULAR COLORS
    BLACK("\u001B[0;30m"),
    RED("\u001B[0;31m"),
    GREEN("\u001B[0;32m"),
    YELLOW("\u001B[0;33m"),
    BLUE("\u001B[0;34m"),
    PURPLE("\u001B[0;35m"),
    CYAN("\u001B[0;36m"),
    WHITE("\u001B[0;37m"),
    // BOLD
    BLACK_BOLD("\u001B[1;30m"),
    RED_BOLD("\u001B[1;31m"),
    GREEN_BOLD("\u001B[1;32m"),
    YELLOW_BOLD("\u001B[1;33m"),
    BLUE_BOLD("\u001B[1;34m"),
    PURPLE_BOLD("\u001B[1;35m"),
    CYAN_BOLD("\u001B[1;36m"),
    WHITE_BOLD("\u001B[1;37m"),
    // UNDERLINE
    BLACK_UNDERLINED("\u001B[4;30m"),
    RED_UNDERLINED("\u001B[4;31m"),
    GREEN_UNDERLINED("\u001B[4;32m"),
    YELLOW_UNDERLINED("\u001B[4;33m"),
    BLUE_UNDERLINED("\u001B[4;34m"),
    PURPLE_UNDERLINED("\u001B[4;35m"),
    CYAN_UNDERLINED("\u001B[4;36m"),
    WHITE_UNDERLINED("\u001B[4;37m"),
    // BACKGROUND COLORS
    BLACK_BG("\u001B[40m"),
    RED_BG("\u001B[41m"),
    GREEN_BG("\u001B[42m"),
    YELLOW_BG("\u001B[43m"),
    BLUE_BG("\u001B[44m"),
    PURPLE_BG("\u001B[45m"),
    CYAN_BG("\u001B[46m"),
    WHITE_BG("\u001B[47m"),
    // HIGH INTENSITY
    BLACK_BRIGHT("\u001B[0;90m"),
    RED_BRIGHT("\u001B[0;91m"),
    GREEN_BRIGHT("\u001B[0;92m"),
    YELLOW_BRIGHT("\u001B[0;93m"),
    BLUE_BRIGHT("\u001B[0;94m"),
    PURPLE_BRIGHT("\u001B[0;95m"),
    CYAN_BRIGHT("\u001B[0;96m"),
    WHITE_BRIGHT("\u001B[0;97m"),
    // BOLD HIGH INTENSITY
    BLACK_BOLD_BRIGHT("\u001B[1;90m"),
    RED_BOLD_BRIGHT("\u001B[1;91m"),
    GREEN_BOLD_BRIGHT("\u001B[1;92m"),
    YELLOW_BOLD_BRIGHT("\u001B[1;93m"),
    BLUE_BOLD_BRIGHT("\u001B[1;94m"),
    PURPLE_BOLD_BRIGHT("\u001B[1;95m"),
    CYAN_BOLD_BRIGHT("\u001B[1;96m"),
    WHITE_BOLD_BRIGHT("\u001B[1;97m"),
    // HIGH INTENSITY BACKGROUNDS
    BLACK_BG_BRIGHT("\u001B[100m"),
    RED_BG_BRIGHT("\u001B[101m"),
    GREEN_BG_BRIGHT("\u001B[102m"),
    YELLOW_BG_BRIGHT("\u001B[103m"),
    BLUE_BG_BRIGHT("\u001B[104m"),
    PURPLE_BG_BRIGHT("\u001B[105m"),
    CYAN_BG_BRIGHT("\u001B[106m"),
    WHITE_BG_BRIGHT("\u001B[107m");

    private static final PlaceholderReplacer REPLACER = new StandardPlaceholderReplacer();

    /**
     * The ANSI escape code value representing the color or style.
     */
    private final String value;

    /**
     * Constructs an {@code AnsiColors} constant with the given ANSI escape code.
     *
     * @param color the ANSI escape code string
     */
    AnsiColors(String color) {
        this.value = color;
    }

    /**
     * Returns the ANSI escape code string for this color or style.
     *
     * @return the ANSI escape code
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the enum constant name of this color or style.
     *
     * @return the name of the enum constant
     */
    public String getName() {
        return name();
    }

    /**
     * Returns the ANSI escape code string.
     *
     * @return the ANSI escape code
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * 🎨 Replaces placeholders in the string with ANSI color codes and formats it.
     *
     * @param string     the string containing color placeholders
     * @param arguments  arguments to substitute via {@link String#formatted(Object...)}
     * @return colored and formatted string
     */
    public static String colorize(String string, Object... arguments) {
        return REPLACER.replace(string, placeholderResolver()).formatted(arguments);
    }

    /**
     * 🔍 Returns a resolver for replacing placeholders with ANSI codes.
     */
    private static PlaceholderResolver placeholderResolver() {
        return AnsiColors::resolve;
    }

    /**
     * 🎯 Resolves a placeholder to the corresponding ANSI code.
     *
     * <p>Allows multiple colors separated by the <code>|</code> character.</p>
     *
     * @param string        color name(s)
     * @param defaultValue  default value if the color is not found
     * @return ANSI code(s) as a single string
     */
    private static String resolve(String string, String defaultValue) {
        return Streamable.of(string.split("\\|"))
                .map(AnsiColors::valueOf).map(AnsiColors::getValue)
                .stream()
                .collect(Collectors.joining());
    }

}
